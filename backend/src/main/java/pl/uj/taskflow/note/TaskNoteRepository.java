package pl.uj.taskflow.note;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskNoteRepository extends JpaRepository<TaskNote, UUID> {

    List<TaskNote> findAllByTaskIdOrderByCreatedAtAsc(UUID taskId);

    boolean existsByIdAndTaskProjectOwnerId(UUID id, UUID ownerId);
}
