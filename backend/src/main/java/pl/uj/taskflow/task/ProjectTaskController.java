package pl.uj.taskflow.task;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.uj.taskflow.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/projects/{projectId}")
@SecurityRequirement(name = "bearerAuth")
public class ProjectTaskController {

    private final TaskService taskService;

    ProjectTaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/tasks")
    List<TaskResponse> list(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable UUID projectId,
        @ModelAttribute TaskListRequest request
    ) {
        return taskService.list(user, projectId, request);
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    TaskResponse create(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable UUID projectId,
        @Valid @RequestBody CreateTaskRequest request
    ) {
        return taskService.create(user, projectId, request);
    }

    @GetMapping("/board")
    BoardResponse board(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable UUID projectId
    ) {
        return taskService.board(user, projectId);
    }
}
