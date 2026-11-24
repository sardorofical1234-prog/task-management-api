package uz.portfolio.taskmanagement.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @Query("""
            select task
            from Task task
            where task.id = :taskId
              and task.owner.id = :ownerId
            """)
    Optional<Task> findOwnedTaskById(
            @Param("taskId") Long taskId,
            @Param("ownerId") Long ownerId
    );
}
