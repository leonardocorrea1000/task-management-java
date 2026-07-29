package br.dev.jcorrea.taskmanagement.auth.dto;

import br.dev.jcorrea.taskmanagement.user.User;
import br.dev.jcorrea.taskmanagement.user.UserRole;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static LoginResponse of(String accessToken, long expiresIn, User user) {
        return new LoginResponse(accessToken, "Bearer", expiresIn, UserResponse.from(user));
    }

    public record UserResponse(UUID id, String name, String email, UserRole role) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
        }
    }
}
