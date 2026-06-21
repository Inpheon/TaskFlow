package pl.uj.taskflow.task;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.uj.taskflow.project.Project;
import pl.uj.taskflow.project.ProjectNotFoundException;
import pl.uj.taskflow.project.ProjectRepository;
import pl.uj.taskflow.security.AuthenticatedUser;

@Service
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final Clock clock;

    TaskService(TaskRepository taskRepository, ProjectRepository projectRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(AuthenticatedUser user, UUID projectId, TaskListRequest request) {
        Project project = findOwnedProject(user, projectId);
        return taskRepository.findAllByProjectIdOrderByPositionAscCreatedAtAsc(project.getId()).stream()
            .filter(task -> matches(task, request))
            .sorted(comparator(request))
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
        log.info(
            "Task created: taskId={}, projectId={}, ownerId={}, status={}, position={}",
            task.getId(),
            projectId,
            user.id(),
            task.getStatus(),
            task.getPosition()
        );

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
        log.info("Task updated: taskId={}, ownerId={}", taskId, user.id());
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
        log.info(
            "Task deleted: taskId={}, projectId={}, ownerId={}, status={}",
            taskId,
            task.getProject().getId(),
            user.id(),
            task.getStatus()
        );
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
            logTaskMove(task, user, sourceStatus);
            return TaskResponse.from(task);
        }

        List<Task> targetColumn = new ArrayList<>(taskRepository
            .findAllByProjectIdAndStatusOrderByPositionAscCreatedAtAsc(projectId, targetStatus));
        validatePosition(request.position(), targetColumn.size());
        targetColumn.add(request.position(), task);
        task.move(targetStatus, request.position());
        reindex(sourceColumn);
        reindex(targetColumn);
        logTaskMove(task, user, sourceStatus);
        return TaskResponse.from(task);
    }

    private Project findOwnedProject(AuthenticatedUser user, UUID projectId) {
        return projectRepository.findByIdAndOwnerId(projectId, user.id())
            .orElseThrow(ProjectNotFoundException::new);
    }

    private boolean matches(Task task, TaskListRequest request) {
        return matchesStatus(task, request)
            && matchesPriority(task, request)
            && matchesDueAfter(task, request)
            && matchesDueBefore(task, request)
            && matchesOverdue(task, request)
            && matchesSearch(task, request);
    }

    private boolean matchesStatus(Task task, TaskListRequest request) {
        return request.getStatus() == null || task.getStatus() == request.getStatus();
    }

    private boolean matchesPriority(Task task, TaskListRequest request) {
        return request.getPriority() == null || task.getPriority() == request.getPriority();
    }

    private boolean matchesDueAfter(Task task, TaskListRequest request) {
        return request.getDueAfter() == null
            || task.getDueDate() != null && !task.getDueDate().isBefore(request.getDueAfter());
    }

    private boolean matchesDueBefore(Task task, TaskListRequest request) {
        return request.getDueBefore() == null
            || task.getDueDate() != null && !task.getDueDate().isAfter(request.getDueBefore());
    }

    private boolean matchesOverdue(Task task, TaskListRequest request) {
        return request.getOverdue() == null
            || TaskRules.isOverdue(task, today()) == request.getOverdue();
    }

    private boolean matchesSearch(Task task, TaskListRequest request) {
        String query = normalizeQuery(request.getQ());
        if (query == null) {
            return true;
        }

        return containsIgnoreCase(task.getTitle(), query)
            || containsIgnoreCase(task.getDescription(), query);
    }

    private String normalizeQuery(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private Comparator<Task> comparator(TaskListRequest request) {
        if (request.getSort() == null || request.getSort().isBlank()) {
            if (request.getDirection() != null && !request.getDirection().isBlank()) {
                throw new InvalidTaskListQueryException("direction requires sort");
            }
            return boardOrderComparator();
        }

        TaskListSort sort = TaskListSort.from(request.getSort().trim());
        TaskListSortDirection direction = request.getDirection() == null || request.getDirection().isBlank()
            ? TaskListSortDirection.ASC
            : TaskListSortDirection.from(request.getDirection().trim());

        Comparator<Task> comparator = switch (sort) {
            case CREATED_AT -> Comparator.comparing(Task::getCreatedAt);
            case DUE_DATE -> dueDateComparator(direction);
            case PRIORITY -> Comparator.comparingInt(task -> task.getPriority().ordinal());
            case TITLE -> Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);
            case STATUS -> Comparator.comparingInt(task -> task.getStatus().ordinal());
        };

        if (direction == TaskListSortDirection.DESC && sort != TaskListSort.DUE_DATE) {
            comparator = comparator.reversed();
        }

        return comparator.thenComparing(boardOrderComparator());
    }

    private Comparator<Task> boardOrderComparator() {
        return Comparator
            .comparingInt((Task task) -> task.getStatus().ordinal())
            .thenComparingInt(Task::getPosition)
            .thenComparing(Task::getCreatedAt)
            .thenComparing(Task::getId);
    }

    private Comparator<Task> dueDateComparator(TaskListSortDirection direction) {
        Comparator<LocalDate> dateComparator = direction == TaskListSortDirection.DESC
            ? Comparator.reverseOrder()
            : Comparator.naturalOrder();

        return Comparator
            .comparing((Task task) -> task.getDueDate() == null)
            .thenComparing(Task::getDueDate, Comparator.nullsLast(dateComparator));
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private void logTaskMove(Task task, AuthenticatedUser user, TaskStatus sourceStatus) {
        log.info(
            "Task moved: taskId={}, projectId={}, ownerId={}, sourceStatus={}, targetStatus={}, position={}",
            task.getId(),
            task.getProject().getId(),
            user.id(),
            sourceStatus,
            task.getStatus(),
            task.getPosition()
        );
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
