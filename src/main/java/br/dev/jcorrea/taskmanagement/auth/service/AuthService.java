package br.dev.jcorrea.taskmanagement.auth.service;

import br.dev.jcorrea.taskmanagement.auth.dto.LoginRequest;
import br.dev.jcorrea.taskmanagement.auth.dto.LoginResponse;
import br.dev.jcorrea.taskmanagement.auth.dto.SignupRequest;
import br.dev.jcorrea.taskmanagement.exception.BusinessException;
import br.dev.jcorrea.taskmanagement.security.JwtService;
import br.dev.jcorrea.taskmanagement.user.User;
import br.dev.jcorrea.taskmanagement.user.UserRepository;
import br.dev.jcorrea.taskmanagement.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("E-mail já cadastrado");
        }
        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()), UserRole.USER);
        User saved = userRepository.save(user);
        log.info("Usuário cadastrado: {}", saved.getId());
        return LoginResponse.of(jwtService.generateToken(saved), jwtService.getExpirationSeconds(), saved);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.info("Falha de autenticação para e-mail normalizado");
            throw new BadCredentialsException("Credenciais inválidas");
        }
        return LoginResponse.of(jwtService.generateToken(user), jwtService.getExpirationSeconds(), user);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
