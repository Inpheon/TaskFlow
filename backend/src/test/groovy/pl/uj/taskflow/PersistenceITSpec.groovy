package pl.uj.taskflow

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import pl.uj.taskflow.note.TaskNote
import pl.uj.taskflow.note.TaskNoteRepository
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

import java.time.LocalDate

@SpringBootTest
@Testcontainers
class PersistenceITSpec extends Specification {

    @Shared
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")

    @Autowired
    UserRepository userRepository

    @Autowired
    ProjectRepository projectRepository

    @Autowired
    TaskRepository taskRepository

    @Autowired
    TaskNoteRepository taskNoteRepository

    def "flyway schema supports persisting the core domain graph"() {
        given:
        def user = userRepository.save(new User("demo@example.com", "{noop}demo1234", "Demo User"))
        def project = projectRepository.save(new Project(user, "PAI Project", "Architecture-first app"))
        def task = taskRepository.save(new Task(
            project,
            "Prepare persistence",
            "Create schema and entities",
            TaskPriority.HIGH,
            LocalDate.now().plusDays(3),
            0
        ))
        def note = taskNoteRepository.save(new TaskNote(task, user, "Persistence smoke test"))

        expect:
        userRepository.existsByEmail("demo@example.com")
        projectRepository.existsByIdAndOwnerId(project.id, user.id)
        taskRepository.existsByIdAndProjectOwnerId(task.id, user.id)
        taskNoteRepository.existsByIdAndTaskProjectOwnerId(note.id, user.id)

        and:
        taskRepository.findAllByProjectIdAndStatusOrderByPositionAscCreatedAtAsc(project.id, TaskStatus.TODO)*.title ==
            ["Prepare persistence"]
        taskNoteRepository.findAllByTaskIdOrderByCreatedAtAsc(task.id)*.content == ["Persistence smoke test"]
    }
}
