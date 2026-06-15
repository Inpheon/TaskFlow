package pl.uj.taskflow.task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.uj.taskflow.project.Project;
import pl.uj.taskflow.project.ProjectNotFoundException;
import pl.uj.taskflow.project.ProjectRepository;
import pl.uj.taskflow.security.AuthenticatedUser;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    TaskService(TaskRepository taskRepository, ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(AuthenticatedUser user, UUID projectId) {
        Project project = findOwnedProject(user, projectId);
        return taskRepository.findAllByProjectIdOrderByPositionAscCreatedAtAsc(project.getId()).stream()
            .sorted(Comparator
                .comparingInt((Task task) -> task.getStatus().ordinal())
                .thenComparingInt(Task::getPosition)
                .thenComparing(Task::getCreatedAt))
            .map(TaskResponse::from)
            .toList();
    }

    @Transactional
    public TaskResponse create(AuthenticatedUser user, UUID projectId, CreateTaskRequest request) {
        Project project = findOwnedProjectForUpdate(user, projectId);
        int position = Math.toIntExact(taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.TODO));
        TaskPriority priority = request.priority() == null ? TaskPriority.MEDIUM : request.priority();
        Task task = taskRepository.save(new Task(
            project,
            normalizeRequired(request.title()),
            normalizeOptional(request.description()),
            priority,
            request.dueDate(),
            position
        ));

        return TaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(AuthenticatedUser user, UUID taskId) {
        return TaskResponse.from(findOwnedTask(user, taskId));
    }

    @Transactional
    public TaskResponse update(AuthenticatedUser user, UUID taskId, UpdateTaskRequest request) {
        Task task = findOwnedTask(user, taskId);
        task.updateDetails(
            normalizeRequired(request.title()),
            normalizeOptional(request.description()),
            request.priority(),
            request.dueDate()
        );
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(AuthenticatedUser user, UUID taskId) {
        lockOwnedTaskProject(user, taskId);
        Task task = findOwnedTask(user, taskId);
        List<Task> column = new ArrayList<>(taskRepository
            .findAllByProjectIdAndStatusOrderByPositionAscCreatedAtAsc(
                task.getProject().getId(),
                task.getStatus()
            ));
        column.removeIf(item -> item.getId().equals(task.getId()));
        taskRepository.delete(task);
        reindex(column);
    }

    @Transactional(readOnly = true)
    public BoardResponse board(AuthenticatedUser user, UUID projectId) {
        Project project = findOwnedProject(user, projectId);
        Map<TaskStatus, List<TaskResponse>> columns = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            columns.put(status, new ArrayList<>());
        }
        for (Task task : taskRepository.findAllByProjectIdOrderByPositionAscCreatedAtAsc(projectId)) {
            columns.get(task.getStatus()).add(TaskResponse.from(task));
        }
        return new BoardResponse(BoardProjectResponse.from(project), columns);
    }

    @Transactional
    public TaskResponse move(AuthenticatedUser user, UUID taskId, MoveTaskRequest request) {
        lockOwnedTaskProject(user, taskId);
        Task task = findOwnedTask(user, taskId);
        UUID projectId = task.getProject().getId();
        TaskStatus sourceStatus = task.getStatus();
        TaskStatus targetStatus = request.targetStatus();

        List<Task> sourceColumn = new ArrayList<>(taskRepository
            .findAllByProjectIdAndStatusOrderByPositionAscCreatedAtAsc(projectId, sourceStatus));
        sourceColumn.removeIf(item -> item.getId().equals(task.getId()));

        if (sourceStatus == targetStatus) {
            validatePosition(request.position(), sourceColumn.size());
            sourceColumn.add(request.position(), task);
            task.move(targetStatus, request.position());
            reindex(sourceColumn);
            return TaskResponse.from(task);
        }

        List<Task> targetColumn = new ArrayList<>(taskRepository
            .findAllByProjectIdAndStatusOrderByPositionAscCreatedAtAsc(projectId, targetStatus));
        validatePosition(request.position(), targetColumn.size());
        targetColumn.add(request.position(), task);
        task.move(targetStatus, request.position());
        reindex(sourceColumn);
        reindex(targetColumn);
        return TaskResponse.from(task);
    }

    private Project findOwnedProject(AuthenticatedUser user, UUID projectId) {
        return projectRepository.findByIdAndOwnerId(projectId, user.id())
            .orElseThrow(ProjectNotFoundException::new);
    }

    private Project findOwnedProjectForUpdate(AuthenticatedUser user, UUID projectId) {
        return projectRepository.findByIdAndOwnerIdForUpdate(projectId, user.id())
            .orElseThrow(ProjectNotFoundException::new);
    }

    private void lockOwnedTaskProject(AuthenticatedUser user, UUID taskId) {
        projectRepository.findByTaskIdAndOwnerIdForUpdate(taskId, user.id())
            .orElseThrow(TaskNotFoundException::new);
    }

    private Task findOwnedTask(AuthenticatedUser user, UUID taskId) {
        return taskRepository.findByIdAndProjectOwnerId(taskId, user.id())
            .orElseThrow(TaskNotFoundException::new);
    }

    private void validatePosition(int position, int maximum) {
        if (position > maximum) {
            throw new InvalidTaskPositionException(position, maximum);
        }
    }

    private void reindex(List<Task> tasks) {
        for (int index = 0; index < tasks.size(); index++) {
            tasks.get(index).reposition(index);
        }
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
