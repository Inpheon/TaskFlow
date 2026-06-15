package pl.uj.taskflow.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    UUID projectId,
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    LocalDate dueDate,
    int position,
    Instant createdAt,
    Instant updatedAt,
    Instant completedAt
) {

    static TaskResponse from(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getProject().getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getPriority(),
            task.getDueDate(),
            task.getPosition(),
            task.getCreatedAt(),
            task.getUpdatedAt(),
            task.getCompletedAt()
        );
    }
}
