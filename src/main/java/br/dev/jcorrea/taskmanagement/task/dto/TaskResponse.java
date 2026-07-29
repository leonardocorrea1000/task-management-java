package br.dev.jcorrea.taskmanagement.task.dto;

import br.dev.jcorrea.taskmanagement.task.Task;
import br.dev.jcorrea.taskmanagement.task.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
