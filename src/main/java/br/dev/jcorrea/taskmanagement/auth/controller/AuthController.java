package br.dev.jcorrea.taskmanagement.auth.controller;

import br.dev.jcorrea.taskmanagement.auth.dto.LoginRequest;
import br.dev.jcorrea.taskmanagement.auth.dto.LoginResponse;
import br.dev.jcorrea.taskmanagement.auth.dto.SignupRequest;
import br.dev.jcorrea.taskmanagement.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar usuário")
    @SecurityRequirements
    public LoginResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário")
    @SecurityRequirements
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
