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
import pl.uj.taskflow.note.TaskNote
import pl.uj.taskflow.note.TaskNoteRepository
import pl.uj.taskflow.project.Project
import pl.uj.taskflow.project.ProjectRepository
import pl.uj.taskflow.task.Task
import pl.uj.taskflow.task.TaskPriority
import pl.uj.taskflow.task.TaskRepository
import pl.uj.taskflow.user.User
import pl.uj.taskflow.user.UserRepository
import spock.lang.Shared
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TaskNoteITSpec extends Specification {

    @Shared
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")

    @Autowired
    MockMvc mockMvc

    @Autowired
    UserRepository userRepository

    @Autowired
    ProjectRepository projectRepository

    @Autowired
    TaskRepository taskRepository

    @Autowired
    TaskNoteRepository taskNoteRepository

    ObjectMapper objectMapper = new ObjectMapper()

    def cleanup() {
        taskNoteRepository.deleteAll()
        taskRepository.deleteAll()
        projectRepository.deleteAll()
        userRepository.deleteAll()
    }

    def "create note stores current user as author"() {
        given:
        JsonNode session = registerUser("owner@example.com", "Owner User")
        Task task = task("owner@example.com", "Task")

        expect:
        mockMvc.perform(post("/api/tasks/${task.id}/notes")
            .header("Authorization", bearer(session))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "content": " First note "
                }
            """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath('$.id').exists())
            .andExpect(jsonPath('$.taskId').value(task.id.toString()))
            .andExpect(jsonPath('$.authorId').value(user("owner@example.com").id.toString()))
            .andExpect(jsonPath('$.authorDisplayName').value("Owner User"))
            .andExpect(jsonPath('$.content').value("First note"))
            .andExpect(jsonPath('$.createdAt').exists())

        and:
        taskNoteRepository.findAllByTaskIdOrderByCreatedAtAsc(task.id)*.content == ["First note"]
    }

    def "create note rejects blank content"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        Task task = task("owner@example.com", "Task")

        expect:
        mockMvc.perform(post("/api/tasks/${task.id}/notes")
            .header("Authorization", bearer(session))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "content": "   "
                }
            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath('$.error').value("Validation error"))
    }

    def "create note hides task owned by another user"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        registerUser("other@example.com")
        Task task = task("other@example.com", "Other task")

        expect:
        mockMvc.perform(post("/api/tasks/${task.id}/notes")
            .header("Authorization", bearer(session))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "content": "Hidden note"
                }
            """))
            .andExpect(status().isNotFound())

        and:
        taskNoteRepository.count() == 0
    }

    def "list notes returns notes in creation order"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = user("owner@example.com")
        Task task = task("owner@example.com", "Task")
        taskNoteRepository.save(new TaskNote(task, owner, "First"))
        taskNoteRepository.save(new TaskNote(task, owner, "Second"))

        expect:
        mockMvc.perform(get("/api/tasks/${task.id}/notes")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$[0].content').value("First"))
            .andExpect(jsonPath('$[1].content').value("Second"))
            .andExpect(jsonPath('$[2]').doesNotExist())
    }

    def "list notes hides task owned by another user"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        registerUser("other@example.com")
        Task task = task("other@example.com", "Other task")
        taskNoteRepository.save(new TaskNote(task, user("other@example.com"), "Hidden"))

        expect:
        mockMvc.perform(get("/api/tasks/${task.id}/notes")
            .header("Authorization", bearer(session)))
            .andExpect(status().isNotFound())
    }

    def "delete note removes note from owned task"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = user("owner@example.com")
        Task task = task("owner@example.com", "Task")
        TaskNote note = taskNoteRepository.save(new TaskNote(task, owner, "Delete me"))

        expect:
        mockMvc.perform(delete("/api/notes/${note.id}")
            .header("Authorization", bearer(session)))
            .andExpect(status().isNoContent())

        and:
        !taskNoteRepository.existsById(note.id)
    }

    def "delete note hides note owned by another user project"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        registerUser("other@example.com")
        Task task = task("other@example.com", "Other task")
        TaskNote note = taskNoteRepository.save(new TaskNote(task, user("other@example.com"), "Hidden"))

        expect:
        mockMvc.perform(delete("/api/notes/${note.id}")
            .header("Authorization", bearer(session)))
            .andExpect(status().isNotFound())

        and:
        taskNoteRepository.existsById(note.id)
    }

    def "note endpoints require authentication"() {
        given:
        registerUser("owner@example.com")
        Task task = task("owner@example.com", "Task")
        TaskNote note = taskNoteRepository.save(new TaskNote(task, user("owner@example.com"), "Note"))

        expect:
        mockMvc.perform(get("/api/tasks/${task.id}/notes"))
            .andExpect(status().isUnauthorized())

        and:
        mockMvc.perform(post("/api/tasks/${task.id}/notes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "content": "Note"
                }
            """))
            .andExpect(status().isUnauthorized())

        and:
        mockMvc.perform(delete("/api/notes/${note.id}"))
            .andExpect(status().isUnauthorized())
    }

    private Task task(String email, String title) {
        User owner = user(email)
        Project project = projectRepository.save(new Project(owner, "Project ${email}", null))
        return taskRepository.save(new Task(project, title, null, TaskPriority.MEDIUM, null, 0))
    }

    private User user(String email) {
        return userRepository.findByEmail(email).orElseThrow()
    }

    private JsonNode registerUser(String email, String displayName = "Demo User") {
        String response = mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "${email}",
                  "password": "demo1234",
                  "displayName": "${displayName}"
                }
            """))
            .andReturn()
            .response
            .contentAsString

        return objectMapper.readTree(response)
    }

    private String bearer(JsonNode session) {
        return "Bearer ${session.get("accessToken").asText()}"
    }
}
