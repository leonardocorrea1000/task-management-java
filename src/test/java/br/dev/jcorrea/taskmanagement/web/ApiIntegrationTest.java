package br.dev.jcorrea.taskmanagement.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.dev.jcorrea.taskmanagement.task.Task;
import br.dev.jcorrea.taskmanagement.task.TaskRepository;
import br.dev.jcorrea.taskmanagement.task.TaskStatus;
import br.dev.jcorrea.taskmanagement.user.User;
import br.dev.jcorrea.taskmanagement.user.UserRepository;
import br.dev.jcorrea.taskmanagement.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:task_management;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "app.jwt.secret=test-secret-with-at-least-32-bytes-for-jwt",
        "app.jwt.expiration=3600",
        "app.rate-limit.signup.capacity=100",
        "app.rate-limit.signup.window=60s",
        "app.rate-limit.login.capacity=10",
        "app.rate-limit.login.window=60s",
        "app.rate-limit.api.capacity=100",
        "app.rate-limit.api.window=60s"
})
@Transactional
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void healthIsPublicAndTasksRequireToken() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/tasks"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void signupAndLoginRejectUnsupportedGetMethods() throws Exception {
        mockMvc.perform(get("/api/signup"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("POST")))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"))
                .andExpect(jsonPath("$.message").value("Método HTTP GET não permitido para esta rota"))
                .andExpect(jsonPath("$.path").value("/api/signup"))
                .andExpect(jsonPath("$.timestamp").exists());

        mockMvc.perform(get("/api/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("POST")))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"))
                .andExpect(jsonPath("$.message").value("Método HTTP GET não permitido para esta rota"))
                .andExpect(jsonPath("$.path").value("/api/login"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void signupValidationReturnsStandardBadRequest() throws Exception {
        mockMvc.perform(post("/api/signup")
                        .contentType("application/json")
                        .content("""
                                {"name":"","email":"invalido","password":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Dados inválidos"))
                .andExpect(jsonPath("$.path").value("/api/signup"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors.email", notNullValue()));
    }

    @Test
    void validSignupIsPublic() throws Exception {
        mockMvc.perform(post("/api/signup")
                        .contentType("application/json")
                        .content("""
                                {"name":"Maria Silva","email":"maria@example.com","password":"SenhaSegura123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()));
    }

    @Test
    void invalidJsonReturnsStandardBadRequest() throws Exception {
        mockMvc.perform(post("/api/signup")
                        .contentType("application/json")
                        .content("""
                                {"name":
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Dados inválidos"))
                .andExpect(jsonPath("$.path").value("/api/signup"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void unknownRouteReturnsStandardNotFound() throws Exception {
        mockMvc.perform(get("/rota-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.path").value("/rota-inexistente"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void loginReturnsTokenAndUserCannotAccessAnotherUsersTask() throws Exception {
        User owner = userRepository.save(new User("Owner", "owner@example.com",
                passwordEncoder.encode("SenhaSegura123"), UserRole.USER));
        User another = userRepository.save(new User("Another", "another@example.com",
                passwordEncoder.encode("SenhaSegura123"), UserRole.USER));
        Task othersTask = taskRepository.save(new Task("Privada", null, TaskStatus.PENDING, null, another));

        String response = mockMvc.perform(post("/api/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"owner@example.com","password":"SenhaSegura123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = response.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/tasks/{id}", othersTask.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        assert owner.getId() != null;
    }

    @Test
    void rateLimitReturns429() throws Exception {
        userRepository.save(new User("Leo", "leo@example.com",
                passwordEncoder.encode("SenhaSegura123"), UserRole.USER));

        String payload = """
                {"email":"leo@example.com","password":"SenhaSegura123"}
                """;

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/login").with(request -> {
                        request.setRemoteAddr("10.0.0.123");
                        return request;
                    }).contentType("application/json").content(payload))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/login").with(request -> {
                    request.setRemoteAddr("10.0.0.123");
                    return request;
                }).contentType("application/json").content(payload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
