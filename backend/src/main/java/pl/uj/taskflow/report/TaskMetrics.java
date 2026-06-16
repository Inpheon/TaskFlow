package pl.uj.taskflow.report;

public record TaskMetrics(
    long totalTasks,
    long todoTasks,
    long inProgressTasks,
    long doneTasks,
    int completionPercentage,
    long overdueTasks,
    long highPriorityOpenTasks
) {

    public long openTasks() {
        return todoTasks + inProgressTasks;
    }
}
