package pl.uj.taskflow

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import pl.uj.taskflow.user.User
import pl.uj.taskflow.user.UserRepository
import spock.lang.Shared
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthITSpec extends Specification {

    @Shared
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")

    @Autowired
    MockMvc mockMvc

    @Autowired
    UserRepository userRepository

    @Autowired
    PasswordEncoder passwordEncoder

    ObjectMapper objectMapper = new ObjectMapper()

    def cleanup() {
        userRepository.deleteAll()
    }

    def "register creates user, stores encoded password and returns usable session"() {
        when:
        def response = mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "Demo@Example.com",
                  "password": "demo1234",
                  "displayName": "Demo User"
                }
            """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath('$.accessToken').isNotEmpty())
            .andExpect(jsonPath('$.tokenType').value("Bearer"))
            .andExpect(jsonPath('$.user.id').exists())
            .andExpect(jsonPath('$.user.email').value("demo@example.com"))
            .andExpect(jsonPath('$.user.displayName').value("Demo User"))
            .andReturn()
            .response
            .contentAsString

        then:
        def user = userRepository.findByEmail("demo@example.com").orElseThrow()
        user.passwordHash != "demo1234"
        passwordEncoder.matches("demo1234", user.passwordHash)

        and:
        def token = objectMapper.readTree(response).get("accessToken").asText()
        mockMvc.perform(get("/api/auth/me")
            .header("Authorization", "Bearer ${token}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.email').value("demo@example.com"))
    }

    def "register rejects invalid payload"() {
        expect:
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "not-an-email",
                  "password": "short",
                  "displayName": ""
                }
            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath('$.error').value("Validation error"))
    }

    def "register rejects duplicate email"() {
        given:
        userRepository.save(new User("demo@example.com", passwordEncoder.encode("demo1234"), "Demo User"))

        expect:
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "demo@example.com",
                  "password": "demo1234",
                  "displayName": "Demo User"
                }
            """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath('$.error').value("Conflict"))
    }

    def "login with valid credentials returns bearer token"() {
        given:
        userRepository.save(new User("demo@example.com", passwordEncoder.encode("demo1234"), "Demo User"))

        expect:
        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "demo@example.com",
                  "password": "demo1234"
                }
            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.tokenType').value("Bearer"))
            .andExpect(jsonPath('$.accessToken').isNotEmpty())
            .andExpect(jsonPath('$.user.email').value("demo@example.com"))
            .andExpect(jsonPath('$.user.displayName').value("Demo User"))
    }

    def "login rejects invalid credentials"() {
        given:
        userRepository.save(new User("demo@example.com", passwordEncoder.encode("demo1234"), "Demo User"))

        expect:
        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "demo@example.com",
                  "password": "wrong-password"
                }
            """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath('$.error').value("Unauthorized"))
    }

    def "me returns current user for bearer token"() {
        given:
        userRepository.save(new User("demo@example.com", passwordEncoder.encode("demo1234"), "Demo User"))
        def token = loginToken()

        expect:
        mockMvc.perform(get("/api/auth/me")
            .header("Authorization", "Bearer ${token}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.email').value("demo@example.com"))
            .andExpect(jsonPath('$.displayName').value("Demo User"))
    }

    def "me rejects missing token"() {
        expect:
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
    }

    def "me rejects tampered token"() {
        given:
        userRepository.save(new User("demo@example.com", passwordEncoder.encode("demo1234"), "Demo User"))
        def token = loginToken()
        def tamperedToken = token.substring(0, token.length() - 2) + "xx"

        expect:
        mockMvc.perform(get("/api/auth/me")
            .header("Authorization", "Bearer ${tamperedToken}"))
            .andExpect(status().isUnauthorized())
    }

    private String loginToken() {
        def response = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "demo@example.com",
                  "password": "demo1234"
                }
            """))
            .andReturn()
            .response
            .contentAsString

        objectMapper.readTree(response).get("accessToken").asText()
    }
}
