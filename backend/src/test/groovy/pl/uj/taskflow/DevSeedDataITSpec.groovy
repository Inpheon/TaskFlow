package pl.uj.taskflow

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import pl.uj.taskflow.config.DevSeedData
import pl.uj.taskflow.note.TaskNoteRepository
import pl.uj.taskflow.project.ProjectRepository
import pl.uj.taskflow.task.TaskRepository
import pl.uj.taskflow.task.TaskStatus
import pl.uj.taskflow.user.UserRepository
import spock.lang.Shared
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers
class DevSeedDataITSpec extends Specification {

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

    @Autowired
    PasswordEncoder passwordEncoder

    def "dev profile creates demo user with sample project tasks and note"() {
        given:
        def demoUser = userRepository.findByEmail(DevSeedData.DEMO_EMAIL).orElseThrow()
        def projects = projectRepository.findAllByOwnerIdOrderByCreatedAtAsc(demoUser.id)
        def project = projects.find { it.name == "Demo Project" }

        expect:
        passwordEncoder.matches(DevSeedData.DEMO_PASSWORD, demoUser.passwordHash)

        and:
        project
        projects.count { it.name == "Demo Project" } == 1

        and:
        taskRepository.findAllByProjectIdAndStatusOrderByPositionAscCreatedAtAsc(project.id, TaskStatus.TODO)*.title ==
            ["Review API contract"]
        taskRepository.findAllByProjectIdAndStatusOrderByPositionAscCreatedAtAsc(project.id, TaskStatus.IN_PROGRESS)*.title ==
            ["Build project dashboard"]
        taskRepository.findAllByProjectIdAndStatusOrderByPositionAscCreatedAtAsc(project.id, TaskStatus.DONE)*.title ==
            ["Create backend skeleton"]

        and:
        taskNoteRepository.count() == 1
    }
}
