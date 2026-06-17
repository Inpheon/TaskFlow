package pl.uj.taskflow.report;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.uj.taskflow.project.Project;
import pl.uj.taskflow.project.ProjectNotFoundException;
import pl.uj.taskflow.project.ProjectRepository;
import pl.uj.taskflow.security.AuthenticatedUser;
import pl.uj.taskflow.task.Task;
import pl.uj.taskflow.task.TaskRepository;

@Service
public class ProjectInsightsService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TaskMetricsCalculator metricsCalculator;
    private final TaskSuggestionSelector suggestionSelector;
    private final Clock clock;

    ProjectInsightsService(
        ProjectRepository projectRepository,
        TaskRepository taskRepository,
        TaskMetricsCalculator metricsCalculator,
        TaskSuggestionSelector suggestionSelector,
        Clock clock
    ) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.metricsCalculator = metricsCalculator;
        this.suggestionSelector = suggestionSelector;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProjectStatsResponse stats(AuthenticatedUser user, UUID projectId) {
        Project project = findOwnedProject(user, projectId);
        TaskMetrics metrics = metricsCalculator.calculate(findTasks(project), currentDate());
        return ProjectStatsResponse.from(metrics);
    }

    @Transactional(readOnly = true)
    public ProjectReportResponse report(AuthenticatedUser user, UUID projectId) {
        Project project = findOwnedProject(user, projectId);
        Instant generatedAt = clock.instant();
        LocalDate currentDate = LocalDate.ofInstant(generatedAt, ZoneOffset.UTC);
        TaskMetrics metrics = metricsCalculator.calculate(findTasks(project), currentDate);
        return new ProjectReportResponse(
            project.getId(),
            project.getName(),
            generatedAt,
            metrics.totalTasks(),
            metrics.doneTasks(),
            metrics.inProgressTasks(),
            metrics.todoTasks(),
            metrics.completionPercentage(),
            metrics.overdueTasks(),
            metrics.highPriorityOpenTasks()
        );
    }

    @Transactional(readOnly = true)
    public Optional<SuggestedTaskResponse> suggestedTask(AuthenticatedUser user, UUID projectId) {
        Project project = findOwnedProject(user, projectId);
        return suggestionSelector.select(findTasks(project), currentDate())
            .map(suggestion -> SuggestedTaskResponse.from(suggestion.task(), suggestion.reason()));
    }

    private LocalDate currentDate() {
        return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private List<Task> findTasks(Project project) {
        return taskRepository.findAllByProjectIdOrderByPositionAscCreatedAtAsc(project.getId());
    }

    private Project findOwnedProject(AuthenticatedUser user, UUID projectId) {
        return projectRepository.findByIdAndOwnerId(projectId, user.id())
            .orElseThrow(ProjectNotFoundException::new);
    }
}
