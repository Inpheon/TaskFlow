package pl.uj.taskflow.task;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
    @NotBlank
    @Size(max = 200)
    String title,

    @Size(max = 5000)
    String description,

    TaskPriority priority,

    LocalDate dueDate
) {
}
