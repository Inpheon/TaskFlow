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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(FixedClockConfiguration)
class InsightsITSpec extends Specification {

    static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z")
    static final LocalDate TODAY = LocalDate.of(2026, 6, 15)

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

    def "stats returns status counts and rounded completion percentage"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        Project project = project("owner@example.com", "Project")
        saveTask(project, "Todo", TaskStatus.TODO)
        saveTask(project, "Progress", TaskStatus.IN_PROGRESS)
        saveTask(project, "Done", TaskStatus.DONE)

        expect:
        mockMvc.perform(get("/api/projects/${project.id}/stats")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.totalTasks').value(3))
            .andExpect(jsonPath('$.todo').value(1))
            .andExpect(jsonPath('$.inProgress').value(1))
            .andExpect(jsonPath('$.done').value(1))
            .andExpect(jsonPath('$.completionPercentage').value(33))
    }

    def "stats returns zeros for empty project"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        Project project = project("owner@example.com", "Empty")

        expect:
        mockMvc.perform(get("/api/projects/${project.id}/stats")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.totalTasks').value(0))
            .andExpect(jsonPath('$.todo').value(0))
            .andExpect(jsonPath('$.inProgress').value(0))
            .andExpect(jsonPath('$.done').value(0))
            .andExpect(jsonPath('$.completionPercentage').value(0))
    }

    def "stats hides project owned by another user"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        registerUser("other@example.com")
        Project project = project("other@example.com", "Other")

        expect:
        mockMvc.perform(get("/api/projects/${project.id}/stats")
            .header("Authorization", bearer(session)))
            .andExpect(status().isNotFound())
    }

    def "report uses fixed time and precise overdue rules"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        Project project = project("owner@example.com", "Report project")
        saveTask(project, "Overdue high", TaskStatus.TODO, TaskPriority.HIGH, TODAY.minusDays(1))
        saveTask(project, "Due today", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, TODAY)
        saveTask(project, "Done overdue date", TaskStatus.DONE, TaskPriority.HIGH, TODAY.minusDays(2))
        saveTask(project, "Future low", TaskStatus.TODO, TaskPriority.LOW, TODAY.plusDays(1))

        expect:
        mockMvc.perform(get("/api/projects/${project.id}/report")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.projectId').value(project.id.toString()))
            .andExpect(jsonPath('$.projectName').value("Report project"))
            .andExpect(jsonPath('$.generatedAt').value(NOW.toString()))
            .andExpect(jsonPath('$.totalTasks').value(4))
            .andExpect(jsonPath('$.doneTasks').value(1))
            .andExpect(jsonPath('$.inProgressTasks').value(1))
            .andExpect(jsonPath('$.todoTasks').value(2))
            .andExpect(jsonPath('$.completionPercentage').value(25))
            .andExpect(jsonPath('$.overdueTasks').value(1))
            .andExpect(jsonPath('$.highPriorityOpenTasks').value(2))
    }

    def "report hides project owned by another user"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        registerUser("other@example.com")
        Project project = project("other@example.com", "Other")

        expect:
        mockMvc.perform(get("/api/projects/${project.id}/report")
            .header("Authorization", bearer(session)))
            .andExpect(status().isNotFound())
    }

    def "suggested task returns deterministic candidate and reason"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        Project project = project("owner@example.com", "Project")
        saveTask(project, "Future high", TaskStatus.TODO, TaskPriority.HIGH, TODAY.plusDays(1))
        Task expected = saveTask(project, "Oldest overdue", TaskStatus.IN_PROGRESS, TaskPriority.LOW, TODAY.minusDays(3))
        saveTask(project, "Recent overdue", TaskStatus.TODO, TaskPriority.HIGH, TODAY.minusDays(1))
        saveTask(project, "Completed", TaskStatus.DONE, TaskPriority.HIGH, TODAY.minusDays(10))

        expect:
        mockMvc.perform(get("/api/projects/${project.id}/suggested-next-task")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.task.id').value(expected.id.toString()))
            .andExpect(jsonPath('$.task.title').value("Oldest overdue"))
            .andExpect(jsonPath('$.task.status').value("IN_PROGRESS"))
            .andExpect(jsonPath('$.task.priority').value("LOW"))
            .andExpect(jsonPath('$.task.dueDate').value(TODAY.minusDays(3).toString()))
            .andExpect(jsonPath('$.reason').value("OVERDUE"))
    }

    def "suggested task returns no content when project has no open tasks"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        Project project = project("owner@example.com", "Project")
        saveTask(project, "Done", TaskStatus.DONE)

        expect:
        mockMvc.perform(get("/api/projects/${project.id}/suggested-next-task")
            .header("Authorization", bearer(session)))
            .andExpect(status().isNoContent())
    }

    def "suggested task hides project owned by another user"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        registerUser("other@example.com")
        Project project = project("other@example.com", "Other")

        expect:
        mockMvc.perform(get("/api/projects/${project.id}/suggested-next-task")
            .header("Authorization", bearer(session)))
            .andExpect(status().isNotFound())
    }

    def "dashboard aggregates only projects owned by current user"() {
        given:
        JsonNode session = registerUser("owner@example.com")
        registerUser("other@example.com")
        Project first = project("owner@example.com", "First")
        Project second = project("owner@example.com", "Second")
        Project other = project("other@example.com", "Other")
        saveTask(first, "Owner overdue", TaskStatus.TODO, TaskPriority.HIGH, TODAY.minusDays(1))
        saveTask(first, "Owner done", TaskStatus.DONE, TaskPriority.HIGH, TODAY.minusDays(2))
        saveTask(second, "Owner progress", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, TODAY.plusDays(1))
        saveTask(other, "Other overdue", TaskStatus.TODO, TaskPriority.HIGH, TODAY.minusDays(3))

        expect:
        mockMvc.perform(get("/api/dashboard/summary")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.projectsCount').value(2))
            .andExpect(jsonPath('$.openTasksCount').value(2))
            .andExpect(jsonPath('$.doneTasksCount').value(1))
            .andExpect(jsonPath('$.overdueTasksCount').value(1))
            .andExpect(jsonPath('$.highPriorityOpenTasksCount').value(1))
    }

    def "dashboard returns zeros for user without projects"() {
        given:
        JsonNode session = registerUser("owner@example.com")

        expect:
        mockMvc.perform(get("/api/dashboard/summary")
            .header("Authorization", bearer(session)))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.projectsCount').value(0))
            .andExpect(jsonPath('$.openTasksCount').value(0))
            .andExpect(jsonPath('$.doneTasksCount').value(0))
            .andExpect(jsonPath('$.overdueTasksCount').value(0))
            .andExpect(jsonPath('$.highPriorityOpenTasksCount').value(0))
    }

    def "insight endpoints require authentication"() {
        expect:
        mockMvc.perform(get(path))
            .andExpect(status().isUnauthorized())

        where:
        path << [
            "/api/projects/${UUID.randomUUID()}/stats",
            "/api/projects/${UUID.randomUUID()}/report",
            "/api/projects/${UUID.randomUUID()}/suggested-next-task",
            "/api/dashboard/summary"
        ]
    }

    private Project project(String email, String name) {
        User owner = userRepository.findByEmail(email).orElseThrow()
        return projectRepository.save(new Project(owner, name, null))
    }

    private Task saveTask(
        Project project,
        String title,
        TaskStatus status,
        TaskPriority priority = TaskPriority.MEDIUM,
        LocalDate dueDate = null
    ) {
        int position = Math.toIntExact(taskRepository.countByProjectIdAndStatus(project.id, status))
        Task task = new Task(project, title, null, priority, dueDate, position)
        if (status != TaskStatus.TODO) {
            task.move(status, position)
        }
        return taskRepository.saveAndFlush(task)
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
