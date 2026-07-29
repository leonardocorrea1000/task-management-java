package br.dev.jcorrea.taskmanagement.task;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findByUserId(UUID userId, Pageable pageable);

    Page<Task> findByUserIdAndStatus(UUID userId, TaskStatus status, Pageable pageable);

    Optional<Task> findByIdAndUserId(UUID id, UUID userId);
}
