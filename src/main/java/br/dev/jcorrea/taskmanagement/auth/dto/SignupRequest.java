package br.dev.jcorrea.taskmanagement.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
        String name,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail deve ser válido")
        @Size(max = 180, message = "O e-mail deve ter no máximo 180 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 120, message = "A senha deve ter entre 8 e 120 caracteres")
        String password
) {
}
