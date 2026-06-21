package pl.uj.taskflow.config;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import pl.uj.taskflow.note.TaskNote;
import pl.uj.taskflow.note.TaskNoteRepository;
import pl.uj.taskflow.project.Project;
import pl.uj.taskflow.project.ProjectRepository;
import pl.uj.taskflow.task.Task;
import pl.uj.taskflow.task.TaskPriority;
import pl.uj.taskflow.task.TaskRepository;
import pl.uj.taskflow.task.TaskStatus;
import pl.uj.taskflow.user.User;
import pl.uj.taskflow.user.UserRepository;

@Component
@Profile("dev")
@Slf4j
public class DevSeedData implements ApplicationRunner {

    public static final String DEMO_EMAIL = "demo@taskflow.local";
    public static final String DEMO_PASSWORD = "demo1234";

    private static final String DEMO_PROJECT_NAME = "Demo Project";

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TaskNoteRepository taskNoteRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    DevSeedData(
        UserRepository userRepository,
        ProjectRepository projectRepository,
        TaskRepository taskRepository,
        TaskNoteRepository taskNoteRepository,
        PasswordEncoder passwordEncoder,
        Clock clock
    ) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.taskNoteRepository = taskNoteRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User demoUser = userRepository.findByEmail(DEMO_EMAIL)
            .orElseGet(this::createDemoUser);

        boolean demoProjectExists = projectRepository.findAllByOwnerIdOrderByCreatedAtAsc(demoUser.getId()).stream()
            .anyMatch(project -> DEMO_PROJECT_NAME.equals(project.getName()));
        if (demoProjectExists) {
            return;
        }

        LocalDate today = LocalDate.now(clock);
        Project project = projectRepository.save(new Project(
            demoUser,
            DEMO_PROJECT_NAME,
            "Sample project for local development"
        ));

        Task backlogTask = taskRepository.save(new Task(
            project,
            "Review API contract",
            "Open Swagger UI and inspect the available endpoints",
            TaskPriority.HIGH,
            today.plusDays(1),
            0
        ));

        Task activeTask = new Task(
            project,
            "Build project dashboard",
            "Use stats, report and suggested next task endpoints",
            TaskPriority.MEDIUM,
            today.plusDays(3),
            0
        );
        activeTask.move(TaskStatus.IN_PROGRESS, 0);
        taskRepository.save(activeTask);

        Task doneTask = new Task(
            project,
            "Create backend skeleton",
            "Initialize auth, projects, tasks and persistence",
            TaskPriority.LOW,
            today.minusDays(1),
            0
        );
        doneTask.move(TaskStatus.DONE, 0);
        taskRepository.save(doneTask);

        taskNoteRepository.save(new TaskNote(
            backlogTask,
            demoUser,
            "This seeded note can be displayed as a task comment."
        ));
        log.info("Development seed created: userId={}, projectId={}", demoUser.getId(), project.getId());
    }

    private User createDemoUser() {
        return userRepository.save(new User(
            DEMO_EMAIL,
            passwordEncoder.encode(DEMO_PASSWORD),
            "Demo User"
        ));
    }
}
