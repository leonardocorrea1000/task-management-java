package br.dev.jcorrea.taskmanagement.task.dto;

import br.dev.jcorrea.taskmanagement.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
        @NotNull(message = "O status é obrigatório")
        TaskStatus status
) {
}
