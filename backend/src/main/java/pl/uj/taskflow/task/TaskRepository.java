package pl.uj.taskflow.task;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findAllByProjectIdOrderByStatusAscPositionAscCreatedAtAsc(UUID projectId);

    List<Task> findAllByProjectIdAndStatusOrderByPositionAscCreatedAtAsc(UUID projectId, TaskStatus status);

    boolean existsByIdAndProjectOwnerId(UUID id, UUID ownerId);
}
