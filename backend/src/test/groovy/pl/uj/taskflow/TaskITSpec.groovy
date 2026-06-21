package pl.uj.taskflow

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import pl.uj.taskflow.project.Project
import pl.uj.taskflow.project.ProjectRepository
import pl.uj.taskflow.task.Task
import pl.uj.taskflow.task.TaskPriority
import pl.uj.taskflow.task.TaskRepository
import pl.uj.taskflow.task.TaskStatus
import pl.uj.taskflow.user.User
import pl.uj.taskflow.user.UserRepository
import spock.lang.Shared
import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(FixedClockConfiguration)
class TaskITSpec extends Specification {

    static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z")

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

    ObjectMapper objectMapper = new ObjectMapper()

    def cleanup() {
        taskRepository.deleteAll()
        projectRepository.deleteAll()
        userRepository.deleteAll()
    }

    def "create task applies defaults normalizes fields and appends to todo"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))
        saveTask(project, "Existing task", TaskStatus.TODO, 0)

        expect:
        mockMvc.perform(post("/api/projects/${project.id}/tasks")
            .header("Authorization", bearer(session))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": " New task ",
                  "description": " Description ",
                  "dueDate": "2026-06-20"
                }
            """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath('$.projectId').value(project.id.toString()))
            .andExpect(jsonPath('$.title').value("New task"))
            .andExpect(jsonPath('$.description').value("Description"))
            .andExpect(jsonPath('$.status').value("TODO"))
            .andExpect(jsonPath('$.priority').value("MEDIUM"))
            .andExpect(jsonPath('$.dueDate').value("2026-06-20"))
            .andExpect(jsonPath('$.position').value(1))
            .andExpect(jsonPath('$.completedAt').doesNotExist())

        and:
        taskRepository.findAllByProjectIdAndStatusOrderByPositionAscCreatedAtAsc(project.id, TaskStatus.TODO)*.title ==
            ["Existing task", "New task"]
    }

    def "create task rejects invalid payload"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))

        expect:
        mockMvc.perform(post("/api/projects/${project.id}/tasks")
            .header("Authorization", bearer(session))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "   "
                }
            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath('$.error').value("Validation error"))
    }

    def "create task hides project owned by another user"() {
        given:
        JsonNode ownerSession = registerUser("owner@example.com")
        registerUser("other@example.com")
        User other = userRepository.findByEmail("other@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(other, "Other project", null))

        expect:
        mockMvc.perform(post("/api/projects/${project.id}/tasks")
            .header("Authorization", bearer(ownerSession))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "Hidden task"
                }
            """))
            .andExpect(status().isNotFound())

        and:
        taskRepository.count() == 0
    }

    def "task endpoints require authentication"() {
        given:
        User owner = userRepository.save(new User("owner@example.com", "hash", "Owner"))
        Project project = projectRepository.save(new Project(owner, "Project", null))
        Task task = saveTask(project, "Task", TaskStatus.TODO, 0)

        expect:
        mockMvc.perform(get("/api/tasks/${task.id}"))
            .andExpect(status().isUnauthorized())
    }

    def "list tasks returns owned project tasks in board order"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))
        saveTask(project, "Todo second", TaskStatus.TODO, 1)
        saveTask(project, "Done first", TaskStatus.DONE, 0)
        saveTask(project, "Todo first", TaskStatus.TODO, 0)
        saveTask(project, "In progress first", TaskStatus.IN_PROGRESS, 0)

        expect:
        mockMvc.perform(get("/api/projects/${project.id}/tasks")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$[0].title').value("Todo first"))
            .andExpect(jsonPath('$[1].title').value("Todo second"))
            .andExpect(jsonPath('$[2].title').value("In progress first"))
            .andExpect(jsonPath('$[3].title').value("Done first"))
            .andExpect(jsonPath('$[4]').doesNotExist())
    }

    def "list tasks hides project owned by another user"() {
        given:
        JsonNode ownerSession = registerUser("owner@example.com")
        registerUser("other@example.com")
        User other = userRepository.findByEmail("other@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(other, "Other project", null))
        saveTask(project, "Hidden task", TaskStatus.TODO, 0)

        expect:
        mockMvc.perform(get("/api/projects/${project.id}/tasks")
            .header("Authorization", bearer(ownerSession)))
            .andExpect(status().isNotFound())
    }

    def "list tasks filters by status priority due dates overdue state and query"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))
        saveTask(project, "Write API contract", "diagram and endpoints", TaskStatus.TODO, TaskPriority.HIGH,
            LocalDate.of(2026, 6, 10), 0)
        saveTask(project, "Review frontend", "check screens", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM,
            LocalDate.of(2026, 6, 20), 0)
        saveTask(project, "Archive notes", "diagram done", TaskStatus.DONE, TaskPriority.LOW,
            LocalDate.of(2026, 6, 5), 0)

        expect:
        mockMvc.perform(get("/api/projects/${project.id}/tasks?status=TODO")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$[0].title').value("Write API contract"))
            .andExpect(jsonPath('$[1]').doesNotExist())

        and:
        mockMvc.perform(get("/api/projects/${project.id}/tasks?priority=MEDIUM")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$[0].title').value("Review frontend"))
            .andExpect(jsonPath('$[1]').doesNotExist())

        and:
        mockMvc.perform(get("/api/projects/${project.id}/tasks?dueAfter=2026-06-10&dueBefore=2026-06-20")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$[0].title').value("Write API contract"))
            .andExpect(jsonPath('$[1].title').value("Review frontend"))
            .andExpect(jsonPath('$[2]').doesNotExist())

        and:
        mockMvc.perform(get("/api/projects/${project.id}/tasks?overdue=true")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$[0].title').value("Write API contract"))
            .andExpect(jsonPath('$[1]').doesNotExist())

        and:
        mockMvc.perform(get("/api/projects/${project.id}/tasks?q=DIAGRAM")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$[0].title').value("Write API contract"))
            .andExpect(jsonPath('$[1].title').value("Archive notes"))
            .andExpect(jsonPath('$[2]').doesNotExist())
    }

    def "list tasks combines filters"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))
        saveTask(project, "Prepare diagrams", "architecture", TaskStatus.TODO, TaskPriority.HIGH,
            LocalDate.of(2026, 6, 10), 0)
        saveTask(project, "Prepare release", "architecture", TaskStatus.TODO, TaskPriority.MEDIUM,
            LocalDate.of(2026, 6, 10), 1)
        saveTask(project, "Review diagrams", "architecture", TaskStatus.IN_PROGRESS, TaskPriority.HIGH,
            LocalDate.of(2026, 6, 10), 0)

        expect:
        mockMvc.perform(get("/api/projects/${project.id}/tasks?status=TODO&priority=HIGH&q=diagram")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$[0].title').value("Prepare diagrams"))
            .andExpect(jsonPath('$[1]').doesNotExist())
    }

    def "list tasks sorts by supported fields"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))
        saveTask(project, "Beta", null, TaskStatus.TODO, TaskPriority.LOW, LocalDate.of(2026, 6, 20), 0)
        saveTask(project, "alpha", null, TaskStatus.IN_PROGRESS, TaskPriority.HIGH, LocalDate.of(2026, 6, 10), 0)
        saveTask(project, "Gamma", null, TaskStatus.DONE, TaskPriority.MEDIUM, null, 0)

        expect:
        mockMvc.perform(get("/api/projects/${project.id}/tasks?sort=title")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$[0].title').value("alpha"))
            .andExpect(jsonPath('$[1].title').value("Beta"))
            .andExpect(jsonPath('$[2].title').value("Gamma"))

        and:
        mockMvc.perform(get("/api/projects/${project.id}/tasks?sort=priority&direction=desc")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$[0].title').value("alpha"))
            .andExpect(jsonPath('$[1].title').value("Gamma"))
            .andExpect(jsonPath('$[2].title').value("Beta"))

        and:
        mockMvc.perform(get("/api/projects/${project.id}/tasks?sort=dueDate&direction=desc")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$[0].title').value("Beta"))
            .andExpect(jsonPath('$[1].title').value("alpha"))
            .andExpect(jsonPath('$[2].title').value("Gamma"))

        and:
        mockMvc.perform(get("/api/projects/${project.id}/tasks?sort=status&direction=desc")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$[0].title').value("Gamma"))
            .andExpect(jsonPath('$[1].title').value("alpha"))
            .andExpect(jsonPath('$[2].title').value("Beta"))
    }

    def "list tasks rejects invalid query parameters"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))

        expect:
        mockMvc.perform(get("/api/projects/${project.id}/tasks${query}")
            .header("Authorization", bearer(session)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath('$.error').value("Validation error"))

        where:
        query << [
            "?status=UNKNOWN",
            "?priority=UNKNOWN",
            "?dueBefore=soon",
            "?sort=unknown",
            "?sort=title&direction=sideways",
            "?direction=asc"
        ]
    }

    def "get task returns owned task"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))
        Task task = taskRepository.save(new Task(
            project,
            "Task",
            "Description",
            TaskPriority.HIGH,
            LocalDate.of(2026, 6, 20),
            0
        ))

        expect:
        mockMvc.perform(get("/api/tasks/${task.id}")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.id').value(task.id.toString()))
            .andExpect(jsonPath('$.projectId').value(project.id.toString()))
            .andExpect(jsonPath('$.title').value("Task"))
            .andExpect(jsonPath('$.priority').value("HIGH"))
    }

    def "get task hides task owned by another user"() {
        given:
        JsonNode ownerSession = registerUser("owner@example.com")
        registerUser("other@example.com")
        User other = userRepository.findByEmail("other@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(other, "Other project", null))
        Task task = saveTask(project, "Hidden task", TaskStatus.TODO, 0)

        expect:
        mockMvc.perform(get("/api/tasks/${task.id}")
            .header("Authorization", bearer(ownerSession)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath('$.error').value("Not found"))
    }

    def "update task changes details without changing workflow state"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))
        Task task = saveTask(project, "Old task", TaskStatus.IN_PROGRESS, 0)

        expect:
        mockMvc.perform(put("/api/tasks/${task.id}")
            .header("Authorization", bearer(session))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": " Updated task ",
                  "description": "",
                  "priority": "HIGH",
                  "dueDate": "2026-06-21"
                }
            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.title').value("Updated task"))
            .andExpect(jsonPath('$.description').doesNotExist())
            .andExpect(jsonPath('$.priority').value("HIGH"))
            .andExpect(jsonPath('$.dueDate').value("2026-06-21"))
            .andExpect(jsonPath('$.status').value("IN_PROGRESS"))
            .andExpect(jsonPath('$.position').value(0))

        and:
        Task updated = taskRepository.findById(task.id).orElseThrow()
        updated.status == TaskStatus.IN_PROGRESS
        updated.position == 0
    }

    def "update task hides task owned by another user"() {
        given:
        JsonNode ownerSession = registerUser("owner@example.com")
        registerUser("other@example.com")
        User other = userRepository.findByEmail("other@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(other, "Other project", null))
        Task task = saveTask(project, "Original", TaskStatus.TODO, 0)

        expect:
        mockMvc.perform(put("/api/tasks/${task.id}")
            .header("Authorization", bearer(ownerSession))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "Changed",
                  "priority": "LOW"
                }
            """))
            .andExpect(status().isNotFound())

        and:
        taskRepository.findById(task.id).orElseThrow().title == "Original"
    }

    def "delete task compacts its column"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))
        saveTask(project, "First", TaskStatus.TODO, 0)
        Task second = saveTask(project, "Second", TaskStatus.TODO, 1)
        saveTask(project, "Third", TaskStatus.TODO, 2)

        expect:
        mockMvc.perform(delete("/api/tasks/${second.id}")
            .header("Authorization", bearer(session)))
            .andExpect(status().isNoContent())

        and:
        List<Task> tasks = taskRepository
            .findAllByProjectIdAndStatusOrderByPositionAscCreatedAtAsc(project.id, TaskStatus.TODO)
        tasks*.title == ["First", "Third"]
        tasks*.position == [0, 1]
    }

    def "delete task hides task owned by another user"() {
        given:
        JsonNode ownerSession = registerUser("owner@example.com")
        registerUser("other@example.com")
        User other = userRepository.findByEmail("other@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(other, "Other project", null))
        Task task = saveTask(project, "Hidden task", TaskStatus.TODO, 0)

        expect:
        mockMvc.perform(delete("/api/tasks/${task.id}")
            .header("Authorization", bearer(ownerSession)))
            .andExpect(status().isNotFound())

        and:
        taskRepository.existsById(task.id)
    }

    def "board groups tasks into complete ordered columns"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))
        saveTask(project, "Todo", TaskStatus.TODO, 0)
        saveTask(project, "Done", TaskStatus.DONE, 0)

        expect:
        mockMvc.perform(get("/api/projects/${project.id}/board")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.project.id').value(project.id.toString()))
            .andExpect(jsonPath('$.project.name').value("Project"))
            .andExpect(jsonPath('$.columns.TODO[0].title').value("Todo"))
            .andExpect(jsonPath('$.columns.IN_PROGRESS').isEmpty())
            .andExpect(jsonPath('$.columns.DONE[0].title').value("Done"))
    }

    def "board hides project owned by another user"() {
        given:
        JsonNode ownerSession = registerUser("owner@example.com")
        registerUser("other@example.com")
        User other = userRepository.findByEmail("other@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(other, "Other project", null))

        expect:
        mockMvc.perform(get("/api/projects/${project.id}/board")
            .header("Authorization", bearer(ownerSession)))
            .andExpect(status().isNotFound())
    }

    def "move task between columns compacts source and inserts at target position"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))
        saveTask(project, "Todo first", TaskStatus.TODO, 0)
        Task moved = saveTask(project, "Todo second", TaskStatus.TODO, 1)
        saveTask(project, "Todo third", TaskStatus.TODO, 2)
        saveTask(project, "Progress first", TaskStatus.IN_PROGRESS, 0)

        expect:
        mockMvc.perform(patch("/api/tasks/${moved.id}/move")
            .header("Authorization", bearer(session))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "targetStatus": "IN_PROGRESS",
                  "position": 0
                }
            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.status').value("IN_PROGRESS"))
            .andExpect(jsonPath('$.position').value(0))

        and:
        List<Task> todo = tasks(project, TaskStatus.TODO)
        todo*.title == ["Todo first", "Todo third"]
        todo*.position == [0, 1]

        and:
        List<Task> inProgress = tasks(project, TaskStatus.IN_PROGRESS)
        inProgress*.title == ["Todo second", "Progress first"]
        inProgress*.position == [0, 1]
    }

    def "move task between columns allows appending at target size"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))
        Task moved = saveTask(project, "Todo", TaskStatus.TODO, 0)
        saveTask(project, "Progress", TaskStatus.IN_PROGRESS, 0)

        expect:
        mockMvc.perform(patch("/api/tasks/${moved.id}/move")
            .header("Authorization", bearer(session))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "targetStatus": "IN_PROGRESS",
                  "position": 1
                }
            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.status').value("IN_PROGRESS"))
            .andExpect(jsonPath('$.position').value(1))

        and:
        List<Task> inProgress = tasks(project, TaskStatus.IN_PROGRESS)
        inProgress*.title == ["Progress", "Todo"]
        inProgress*.position == [0, 1]
    }

    def "move task within one column reorders every affected task"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))
        Task first = saveTask(project, "First", TaskStatus.TODO, 0)
        saveTask(project, "Second", TaskStatus.TODO, 1)
        saveTask(project, "Third", TaskStatus.TODO, 2)

        expect:
        mockMvc.perform(patch("/api/tasks/${first.id}/move")
            .header("Authorization", bearer(session))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "targetStatus": "TODO",
                  "position": 2
                }
            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.position').value(2))

        and:
        List<Task> tasks = tasks(project, TaskStatus.TODO)
        tasks*.title == ["Second", "Third", "First"]
        tasks*.position == [0, 1, 2]
    }

    def "move task rejects position outside same column"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))
        Task first = saveTask(project, "First", TaskStatus.TODO, 0)
        saveTask(project, "Second", TaskStatus.TODO, 1)

        expect:
        mockMvc.perform(patch("/api/tasks/${first.id}/move")
            .header("Authorization", bearer(session))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "targetStatus": "TODO",
                  "position": 2
                }
            """))
            .andExpect(status().isBadRequest())

        and:
        List<Task> tasks = tasks(project, TaskStatus.TODO)
        tasks*.title == ["First", "Second"]
        tasks*.position == [0, 1]
    }

    def "moving into done sets completion time and moving out clears it"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))
        Task task = saveTask(project, "Task", TaskStatus.TODO, 0)

        when:
        mockMvc.perform(patch("/api/tasks/${task.id}/move")
            .header("Authorization", bearer(session))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "targetStatus": "DONE",
                  "position": 0
                }
            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.completedAt').exists())

        then:
        taskRepository.findById(task.id).orElseThrow().completedAt != null

        when:
        mockMvc.perform(patch("/api/tasks/${task.id}/move")
            .header("Authorization", bearer(session))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "targetStatus": "TODO",
                  "position": 0
                }
            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.completedAt').doesNotExist())

        then:
        taskRepository.findById(task.id).orElseThrow().completedAt == null
    }

    def "move task rejects position outside target column without changing task"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))
        Task task = saveTask(project, "Task", TaskStatus.TODO, 0)

        expect:
        mockMvc.perform(patch("/api/tasks/${task.id}/move")
            .header("Authorization", bearer(session))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "targetStatus": "IN_PROGRESS",
                  "position": 1
                }
            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath('$.error').value("Validation error"))

        and:
        Task unchanged = taskRepository.findById(task.id).orElseThrow()
        unchanged.status == TaskStatus.TODO
        unchanged.position == 0
    }

    def "move task rejects malformed status"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))
        Task task = saveTask(project, "Task", TaskStatus.TODO, 0)

        expect:
        mockMvc.perform(patch("/api/tasks/${task.id}/move")
            .header("Authorization", bearer(session))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "targetStatus": "UNKNOWN",
                  "position": 0
                }
            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath('$.error').value("Validation error"))
    }

    def "move task rejects invalid position payload without changing task"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        User owner = userRepository.findByEmail("owner@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(owner, "Project", null))
        Task task = saveTask(project, "Task", TaskStatus.TODO, 0)

        expect:
        mockMvc.perform(patch("/api/tasks/${task.id}/move")
            .header("Authorization", bearer(session))
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath('$.error').value("Validation error"))

        and:
        Task unchanged = taskRepository.findById(task.id).orElseThrow()
        unchanged.status == TaskStatus.TODO
        unchanged.position == 0

        where:
        requestBody << [
            '{"targetStatus":"IN_PROGRESS","position":-1}',
            '{"targetStatus":"IN_PROGRESS"}'
        ]
    }

    def "move task hides task owned by another user"() {
        given:
        JsonNode ownerSession = registerUser("owner@example.com")
        registerUser("other@example.com")
        User other = userRepository.findByEmail("other@example.com").orElseThrow()
        Project project = projectRepository.save(new Project(other, "Other project", null))
        Task task = saveTask(project, "Hidden task", TaskStatus.TODO, 0)

        expect:
        mockMvc.perform(patch("/api/tasks/${task.id}/move")
            .header("Authorization", bearer(ownerSession))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "targetStatus": "DONE",
                  "position": 0
                }
            """))
            .andExpect(status().isNotFound())

        and:
        taskRepository.findById(task.id).orElseThrow().status == TaskStatus.TODO
    }

    private Task saveTask(Project project, String title, TaskStatus status, int position) {
        return saveTask(project, title, null, status, TaskPriority.MEDIUM, null, position)
    }

    private Task saveTask(
        Project project,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        int position
    ) {
        Task task = new Task(project, title, description, priority, dueDate, position)
        if (status != TaskStatus.TODO) {
            task.move(status, position)
        }
        return taskRepository.save(task)
    }

    private List<Task> tasks(Project project, TaskStatus status) {
        return taskRepository.findAllByProjectIdAndStatusOrderByPositionAscCreatedAtAsc(project.id, status)
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

    private String bearer(JsonNode session) {
        return "Bearer ${session.get("accessToken").asText()}"
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC)
        }
    }
}
