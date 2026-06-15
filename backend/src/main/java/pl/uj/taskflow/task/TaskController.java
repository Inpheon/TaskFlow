package pl.uj.taskflow.task;

import java.util.UUID;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.uj.taskflow.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/{taskId}")
    TaskResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID taskId) {
        return taskService.get(user, taskId);
    }

    @PutMapping("/{taskId}")
    TaskResponse update(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable UUID taskId,
        @Valid @RequestBody UpdateTaskRequest request
    ) {
        return taskService.update(user, taskId, request);
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID taskId) {
        taskService.delete(user, taskId);
    }

    @PatchMapping("/{taskId}/move")
    TaskResponse move(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable UUID taskId,
        @Valid @RequestBody MoveTaskRequest request
    ) {
        return taskService.move(user, taskId, request);
    }
}
