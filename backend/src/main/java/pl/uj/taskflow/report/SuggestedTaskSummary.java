package pl.uj.taskflow.report;

import java.time.LocalDate;
import java.util.UUID;

import pl.uj.taskflow.task.Task;
import pl.uj.taskflow.task.TaskPriority;
import pl.uj.taskflow.task.TaskStatus;

public record SuggestedTaskSummary(
    UUID id,
    String title,
    TaskStatus status,
    TaskPriority priority,
    LocalDate dueDate
) {

    static SuggestedTaskSummary from(Task task) {
        return new SuggestedTaskSummary(
            task.getId(),
            task.getTitle(),
            task.getStatus(),
            task.getPriority(),
            task.getDueDate()
        );
    }
}
