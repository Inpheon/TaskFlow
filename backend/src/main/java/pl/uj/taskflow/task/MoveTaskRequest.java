package pl.uj.taskflow.task;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoveTaskRequest(
    @NotNull
    TaskStatus targetStatus,

    @NotNull
    @Min(0)
    Integer position
) {
}
