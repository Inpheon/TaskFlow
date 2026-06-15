package pl.uj.taskflow

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import pl.uj.taskflow.project.Project
import pl.uj.taskflow.project.ProjectRepository
import pl.uj.taskflow.user.User
import pl.uj.taskflow.user.UserRepository
import spock.lang.Shared
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProjectITSpec extends Specification {

    @Shared
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")

    @Autowired
    MockMvc mockMvc

    @Autowired
    UserRepository userRepository

    @Autowired
    ProjectRepository projectRepository

    ObjectMapper objectMapper = new ObjectMapper()

    def cleanup() {
        projectRepository.deleteAll()
        userRepository.deleteAll()
    }

    def "create project stores it for current user"() {
        given:
        String token = registerUser("owner@example.com").get("accessToken").asText()

        expect:
        mockMvc.perform(post("/api/projects")
            .header("Authorization", "Bearer ${token}")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": " PAI Project ",
                  "description": " First project "
                }
            """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath('$.id').exists())
            .andExpect(jsonPath('$.name').value("PAI Project"))
            .andExpect(jsonPath('$.description').value("First project"))
            .andExpect(jsonPath('$.createdAt').exists())
            .andExpect(jsonPath('$.updatedAt').exists())

        and:
        projectRepository.findAllByOwnerIdOrderByCreatedAtAsc(userRepository.findByEmail("owner@example.com").orElseThrow().id)*.name ==
            ["PAI Project"]
    }

    def "create project requires authentication"() {
        expect:
        mockMvc.perform(post("/api/projects")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "PAI Project"
                }
            """))
            .andExpect(status().isUnauthorized())
    }

    def "create project rejects blank name"() {
        given:
        String token = registerUser("owner@example.com").get("accessToken").asText()

        expect:
        mockMvc.perform(post("/api/projects")
            .header("Authorization", "Bearer ${token}")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "   "
                }
            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath('$.error').value("Validation error"))
    }

    def "list projects returns only projects owned by current user"() {
        given:
        JsonNode ownerSession = registerUser("owner@example.com")
        registerUser("other@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        User other = userRepository.findByEmail("other@example.com").orElseThrow()
        projectRepository.save(new Project(owner, "Owner Project", null))
        projectRepository.save(new Project(other, "Other Project", null))

        expect:
        mockMvc.perform(get("/api/projects")
            .header("Authorization", "Bearer ${ownerSession.get("accessToken").asText()}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$[0].name').value("Owner Project"))
            .andExpect(jsonPath('$[1]').doesNotExist())
    }

    def "get project returns owned project"() {
        given:
        JsonNode ownerSession = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Owner Project", "Visible"))

        expect:
        mockMvc.perform(get("/api/projects/${project.id}")
            .header("Authorization", "Bearer ${ownerSession.get("accessToken").asText()}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.id').value(project.id.toString()))
            .andExpect(jsonPath('$.name').value("Owner Project"))
            .andExpect(jsonPath('$.description').value("Visible"))
    }

    def "get project hides project owned by another user"() {
        given:
        JsonNode ownerSession = registerUser("owner@example.com")
        registerUser("other@example.com")
        User other = userRepository.findByEmail("other@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(other, "Other Project", null))

        expect:
        mockMvc.perform(get("/api/projects/${project.id}")
            .header("Authorization", "Bearer ${ownerSession.get("accessToken").asText()}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath('$.error').value("Not found"))
    }

    def "update project changes only owned project"() {
        given:
        JsonNode ownerSession = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Old Project", "Old description"))

        expect:
        mockMvc.perform(put("/api/projects/${project.id}")
            .header("Authorization", "Bearer ${ownerSession.get("accessToken").asText()}")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Updated Project",
                  "description": ""
                }
            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.name').value("Updated Project"))
            .andExpect(jsonPath('$.description').doesNotExist())

        and:
        Project updated = projectRepository.findById(project.id).orElseThrow()
        updated.name == "Updated Project"
        updated.description == null
    }

    def "update project hides project owned by another user"() {
        given:
        JsonNode ownerSession = registerUser("owner@example.com")
        registerUser("other@example.com")
        User other = userRepository.findByEmail("other@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(other, "Other Project", null))

        expect:
        mockMvc.perform(put("/api/projects/${project.id}")
            .header("Authorization", "Bearer ${ownerSession.get("accessToken").asText()}")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Updated Project"
                }
            """))
            .andExpect(status().isNotFound())

        and:
        projectRepository.findById(project.id).orElseThrow().name == "Other Project"
    }

    def "delete project removes owned project"() {
        given:
        JsonNode ownerSession = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Owner Project", null))

        expect:
        mockMvc.perform(delete("/api/projects/${project.id}")
            .header("Authorization", "Bearer ${ownerSession.get("accessToken").asText()}"))
            .andExpect(status().isNoContent())

        and:
        !projectRepository.existsById(project.id)
    }

    def "delete project hides project owned by another user"() {
        given:
        JsonNode ownerSession = registerUser("owner@example.com")
        registerUser("other@example.com")
        User other = userRepository.findByEmail("other@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(other, "Other Project", null))

        expect:
        mockMvc.perform(delete("/api/projects/${project.id}")
            .header("Authorization", "Bearer ${ownerSession.get("accessToken").asText()}"))
            .andExpect(status().isNotFound())

        and:
        projectRepository.existsById(project.id)
    }

    private JsonNode registerUser(String email) {
        String response = mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "${email}",
                  "password": "demo1234",
                  "displayName": "Demo User"
                }
            """))
            .andReturn()
            .response
            .contentAsString

        return objectMapper.readTree(response)
    }
}
