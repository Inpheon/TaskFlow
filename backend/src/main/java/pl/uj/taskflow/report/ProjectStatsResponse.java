package pl.uj.taskflow.report;

public record ProjectStatsResponse(
    long totalTasks,
    long todo,
    long inProgress,
    long done,
    int completionPercentage
) {

    static ProjectStatsResponse from(TaskMetrics metrics) {
        return new ProjectStatsResponse(
            metrics.totalTasks(),
            metrics.todoTasks(),
            metrics.inProgressTasks(),
            metrics.doneTasks(),
            metrics.completionPercentage()
        );
    }
}
