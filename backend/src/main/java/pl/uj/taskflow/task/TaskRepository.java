package pl.uj.taskflow.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findAllByProjectIdOrderByPositionAscCreatedAtAsc(UUID projectId);

    List<Task> findAllByProjectIdAndStatusOrderByPositionAscCreatedAtAsc(UUID projectId, TaskStatus status);

    long countByProjectIdAndStatus(UUID projectId, TaskStatus status);

    Optional<Task> findByIdAndProjectOwnerId(UUID id, UUID ownerId);

    boolean existsByIdAndProjectOwnerId(UUID id, UUID ownerId);
}
