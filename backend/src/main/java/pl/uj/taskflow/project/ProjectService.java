package pl.uj.taskflow.project;

import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.uj.taskflow.security.AuthenticatedUser;
import pl.uj.taskflow.security.AuthenticatedUserNotFoundException;
import pl.uj.taskflow.user.User;
import pl.uj.taskflow.user.UserRepository;

@Service
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(AuthenticatedUser user) {
        return projectRepository.findAllByOwnerIdOrderByCreatedAtAsc(user.id()).stream()
            .map(ProjectResponse::from)
            .toList();
    }

    @Transactional
    public ProjectResponse create(AuthenticatedUser user, ProjectRequest request) {
        User owner = userRepository.findById(user.id())
            .orElseThrow(AuthenticatedUserNotFoundException::new);
        Project project = projectRepository.save(new Project(
            owner,
            normalizeRequired(request.name()),
            normalizeOptional(request.description())
        ));
        log.info("Project created: projectId={}, ownerId={}", project.getId(), user.id());

        return ProjectResponse.from(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(AuthenticatedUser user, UUID projectId) {
        Project project = findOwnedProject(user, projectId);
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse update(AuthenticatedUser user, UUID projectId, ProjectRequest request) {
        Project project = findOwnedProject(user, projectId);
        project.update(normalizeRequired(request.name()), normalizeOptional(request.description()));
        log.info("Project updated: projectId={}, ownerId={}", projectId, user.id());
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(AuthenticatedUser user, UUID projectId) {
        Project project = findOwnedProject(user, projectId);
        projectRepository.delete(project);
        log.info("Project deleted: projectId={}, ownerId={}", projectId, user.id());
    }

    private Project findOwnedProject(AuthenticatedUser user, UUID projectId) {
        return projectRepository.findByIdAndOwnerId(projectId, user.id())
            .orElseThrow(ProjectNotFoundException::new);
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
