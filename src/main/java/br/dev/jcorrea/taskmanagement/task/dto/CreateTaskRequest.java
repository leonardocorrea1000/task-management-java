package br.dev.jcorrea.taskmanagement.task.dto;

import br.dev.jcorrea.taskmanagement.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateTaskRequest(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 160, message = "O título deve ter no máximo 160 caracteres")
        String title,

        @Size(max = 4000, message = "A descrição deve ter no máximo 4000 caracteres")
        String description,

        TaskStatus status,

        LocalDate dueDate
) {
}
