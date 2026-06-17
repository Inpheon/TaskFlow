package pl.uj.taskflow.report;

import java.util.UUID;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.uj.taskflow.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/projects/{projectId}")
@SecurityRequirement(name = "bearerAuth")
public class ProjectInsightsController {

    private final ProjectInsightsService projectInsightsService;

    ProjectInsightsController(ProjectInsightsService projectInsightsService) {
        this.projectInsightsService = projectInsightsService;
    }

    @GetMapping("/stats")
    ProjectStatsResponse stats(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable UUID projectId
    ) {
        return projectInsightsService.stats(user, projectId);
    }

    @GetMapping("/report")
    ProjectReportResponse report(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable UUID projectId
    ) {
        return projectInsightsService.report(user, projectId);
    }

    @GetMapping("/suggested-next-task")
    ResponseEntity<SuggestedTaskResponse> suggestedTask(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable UUID projectId
    ) {
        return projectInsightsService.suggestedTask(user, projectId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
