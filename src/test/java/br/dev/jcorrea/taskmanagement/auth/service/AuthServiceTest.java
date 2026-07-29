package br.dev.jcorrea.taskmanagement.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.dev.jcorrea.taskmanagement.auth.dto.LoginRequest;
import br.dev.jcorrea.taskmanagement.auth.dto.SignupRequest;
import br.dev.jcorrea.taskmanagement.exception.BusinessException;
import br.dev.jcorrea.taskmanagement.security.JwtService;
import br.dev.jcorrea.taskmanagement.user.User;
import br.dev.jcorrea.taskmanagement.user.UserRepository;
import br.dev.jcorrea.taskmanagement.user.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void signupCreatesUserWithBcryptPasswordAndToken() {
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        var response = service.signup(new SignupRequest("Leonardo Correa", "LEO@EXAMPLE.COM", "SenhaSegura123"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("leo@example.com");
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        assertThat(saved.getPassword()).isNotEqualTo("SenhaSegura123");
        assertThat(passwordEncoder.matches("SenhaSegura123", saved.getPassword())).isTrue();
        assertThat(response.accessToken()).isEqualTo("token");
    }

    @Test
    void signupRejectsDuplicatedEmail() {
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService);
        when(userRepository.existsByEmail("leo@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.signup(new SignupRequest("Leo", "leo@example.com", "SenhaSegura123")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("E-mail já cadastrado");
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService);
        User user = new User("Leo", "leo@example.com", passwordEncoder.encode("SenhaSegura123"), UserRole.USER);
        when(userRepository.findByEmail("leo@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        var response = service.login(new LoginRequest("leo@example.com", "SenhaSegura123"));

        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(response.user().email()).isEqualTo("leo@example.com");
    }

    @Test
    void loginRejectsInvalidCredentials() {
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService);
        User user = new User("Leo", "leo@example.com", passwordEncoder.encode("SenhaSegura123"), UserRole.USER);
        when(userRepository.findByEmail("leo@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginRequest("leo@example.com", "errada")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
