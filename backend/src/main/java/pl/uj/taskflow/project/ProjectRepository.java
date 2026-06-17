package pl.uj.taskflow.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findAllByOwnerIdOrderByCreatedAtAsc(UUID ownerId);

    Optional<Project> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select project
        from Project project
        where project.id = :projectId
          and project.owner.id = :ownerId
        """)
    Optional<Project> findByIdAndOwnerIdForUpdate(
        @Param("projectId") UUID projectId,
        @Param("ownerId") UUID ownerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select project
        from Project project, Task task
        where task.project = project
          and task.id = :taskId
          and project.owner.id = :ownerId
        """)
    Optional<Project> findByTaskIdAndOwnerIdForUpdate(
        @Param("taskId") UUID taskId,
        @Param("ownerId") UUID ownerId
    );

    long countByOwnerId(UUID ownerId);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}
