package br.dev.jcorrea.taskmanagement.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final String applicationName;

    public HealthController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check público")
    @SecurityRequirements
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "application", applicationName,
                "timestamp", Instant.now()
        );
    }
}
