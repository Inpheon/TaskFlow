package pl.uj.taskflow.dashboard;

public record DashboardSummaryResponse(
    long projectsCount,
    long openTasksCount,
    long doneTasksCount,
    long overdueTasksCount,
    long highPriorityOpenTasksCount
) {
}
