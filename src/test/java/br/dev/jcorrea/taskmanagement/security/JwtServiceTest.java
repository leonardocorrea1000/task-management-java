package br.dev.jcorrea.taskmanagement.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.jcorrea.taskmanagement.user.User;
import br.dev.jcorrea.taskmanagement.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes-for-jwt";

    @Test
    void generatesExtractsAndValidatesToken() {
        JwtService jwtService = new JwtService(SECRET, 3600);
        User user = new User("Leo", "leo@example.com", "hash", UserRole.USER);
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId().toString());
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void rejectsExpiredOrInvalidToken() {
        JwtService expiredService = new JwtService(SECRET, -1);
        User user = new User("Leo", "leo@example.com", "hash", UserRole.USER);
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();

        String expiredToken = expiredService.generateToken(user);

        assertThat(expiredService.isTokenValid(expiredToken, userDetails)).isFalse();
        assertThatThrownBy(() -> expiredService.extractUserId("invalid-token")).isInstanceOf(RuntimeException.class);
    }
}
