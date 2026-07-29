package br.dev.jcorrea.taskmanagement.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitConfig(
        Limit signup,
        Limit login,
        Limit api
) {
    public record Limit(int capacity, Duration window) {
    }
}
