package pl.uj.taskflow.dashboard;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.uj.taskflow.project.ProjectRepository;
import pl.uj.taskflow.report.TaskMetrics;
import pl.uj.taskflow.report.TaskMetricsCalculator;
import pl.uj.taskflow.security.AuthenticatedUser;
import pl.uj.taskflow.task.TaskRepository;

@Service
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TaskMetricsCalculator metricsCalculator;
    private final Clock clock;

    DashboardService(
        ProjectRepository projectRepository,
        TaskRepository taskRepository,
        TaskMetricsCalculator metricsCalculator,
        Clock clock
    ) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.metricsCalculator = metricsCalculator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(AuthenticatedUser user) {
        TaskMetrics metrics = metricsCalculator.calculate(
            taskRepository.findAllByProjectOwnerId(user.id()),
            LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC)
        );
        return new DashboardSummaryResponse(
            projectRepository.countByOwnerId(user.id()),
            metrics.openTasks(),
            metrics.doneTasks(),
            metrics.overdueTasks(),
            metrics.highPriorityOpenTasks()
        );
    }
}
