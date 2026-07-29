package br.dev.jcorrea.taskmanagement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "Bearer Authentication";

    @Bean
    public OpenAPI taskManagementOpenApi() {
        SecurityScheme securityScheme = new SecurityScheme()
                .name(BEARER_AUTH)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("Task Management API")
                        .description("API REST demonstrativa para gerenciamento de tarefas com JWT, PostgreSQL e Flyway.")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, securityScheme))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
