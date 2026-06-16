package pl.uj.taskflow.note;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.uj.taskflow.security.AuthenticatedUser;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class TaskNoteController {

    private final TaskNoteService taskNoteService;

    TaskNoteController(TaskNoteService taskNoteService) {
        this.taskNoteService = taskNoteService;
    }

    @GetMapping("/api/tasks/{taskId}/notes")
    List<TaskNoteResponse> list(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable UUID taskId
    ) {
        return taskNoteService.list(user, taskId);
    }

    @PostMapping("/api/tasks/{taskId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    TaskNoteResponse create(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable UUID taskId,
        @Valid @RequestBody CreateTaskNoteRequest request
    ) {
        return taskNoteService.create(user, taskId, request);
    }

    @DeleteMapping("/api/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID noteId) {
        taskNoteService.delete(user, noteId);
    }
}
