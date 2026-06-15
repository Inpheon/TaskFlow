package pl.uj.taskflow.task;

import java.util.List;
import java.util.Map;

public record BoardResponse(
    BoardProjectResponse project,
    Map<TaskStatus, List<TaskResponse>> columns
) {
}
