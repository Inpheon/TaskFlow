package pl.uj.taskflow.note;

import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.uj.taskflow.security.AuthenticatedUser;
import pl.uj.taskflow.security.AuthenticatedUserNotFoundException;
import pl.uj.taskflow.task.Task;
import pl.uj.taskflow.task.TaskNotFoundException;
import pl.uj.taskflow.task.TaskRepository;
import pl.uj.taskflow.user.User;
import pl.uj.taskflow.user.UserRepository;

@Service
@Slf4j
public class TaskNoteService {

    private final TaskNoteRepository taskNoteRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    TaskNoteService(
        TaskNoteRepository taskNoteRepository,
        TaskRepository taskRepository,
        UserRepository userRepository
    ) {
        this.taskNoteRepository = taskNoteRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskNoteResponse> list(AuthenticatedUser user, UUID taskId) {
        Task task = findOwnedTask(user, taskId);
        return taskNoteRepository.findAllByTaskIdOrderByCreatedAtAsc(task.getId()).stream()
            .map(TaskNoteResponse::from)
            .toList();
    }

    @Transactional
    public TaskNoteResponse create(AuthenticatedUser user, UUID taskId, CreateTaskNoteRequest request) {
        Task task = findOwnedTask(user, taskId);
        User author = userRepository.findById(user.id())
            .orElseThrow(AuthenticatedUserNotFoundException::new);
        TaskNote note = taskNoteRepository.save(new TaskNote(task, author, request.content().trim()));
        log.info(
            "Task note created: noteId={}, taskId={}, projectId={}, authorId={}",
            note.getId(),
            taskId,
            task.getProject().getId(),
            user.id()
        );
        return TaskNoteResponse.from(note);
    }

    @Transactional
    public void delete(AuthenticatedUser user, UUID noteId) {
        TaskNote note = taskNoteRepository.findByIdAndTaskProjectOwnerId(noteId, user.id())
            .orElseThrow(TaskNoteNotFoundException::new);
        taskNoteRepository.delete(note);
        log.info(
            "Task note deleted: noteId={}, taskId={}, ownerId={}",
            noteId,
            note.getTask().getId(),
            user.id()
        );
    }

    private Task findOwnedTask(AuthenticatedUser user, UUID taskId) {
        return taskRepository.findByIdAndProjectOwnerId(taskId, user.id())
            .orElseThrow(TaskNotFoundException::new);
    }
}
