package pl.uj.taskflow.project;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.uj.taskflow.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    List<ProjectResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return projectService.list(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProjectResponse create(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody ProjectRequest request) {
        return projectService.create(user, request);
    }

    @GetMapping("/{projectId}")
    ProjectResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID projectId) {
        return projectService.get(user, projectId);
    }

    @PutMapping("/{projectId}")
    ProjectResponse update(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable UUID projectId,
        @Valid @RequestBody ProjectRequest request
    ) {
        return projectService.update(user, projectId, request);
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID projectId) {
        projectService.delete(user, projectId);
    }
}
