package pl.uj.taskflow.report;

import java.time.Instant;
import java.util.UUID;

public record ProjectReportResponse(
    UUID projectId,
    String projectName,
    Instant generatedAt,
    long totalTasks,
    long doneTasks,
    long inProgressTasks,
    long todoTasks,
    int completionPercentage,
    long overdueTasks,
    long highPriorityOpenTasks
) {
}
