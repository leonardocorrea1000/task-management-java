package br.dev.jcorrea.taskmanagement.task.controller;

import br.dev.jcorrea.taskmanagement.task.TaskService;
import br.dev.jcorrea.taskmanagement.task.TaskStatus;
import br.dev.jcorrea.taskmanagement.task.dto.CreateTaskRequest;
import br.dev.jcorrea.taskmanagement.task.dto.TaskResponse;
import br.dev.jcorrea.taskmanagement.task.dto.UpdateTaskRequest;
import br.dev.jcorrea.taskmanagement.task.dto.UpdateTaskStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "Listar tarefas do usuário autenticado")
    public Page<TaskResponse> list(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestParam(required = false) TaskStatus status,
                                   @PageableDefault(size = 20, sort = "createdAt",
                                           direction = Sort.Direction.DESC) Pageable pageable) {
        return taskService.list(userDetails.getUsername(), status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar tarefa do usuário autenticado")
    public TaskResponse getById(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID id) {
        return taskService.getById(userDetails.getUsername(), id);
    }

    @PostMapping
    @Operation(summary = "Criar tarefa para o usuário autenticado")
    public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal UserDetails userDetails,
                                               @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = taskService.create(userDetails.getUsername(), request);
        return ResponseEntity.created(URI.create("/api/tasks/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Substituir campos editáveis de uma tarefa")
    public TaskResponse update(@AuthenticationPrincipal UserDetails userDetails,
                               @PathVariable UUID id,
                               @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(userDetails.getUsername(), id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status de uma tarefa")
    public TaskResponse updateStatus(@AuthenticationPrincipal UserDetails userDetails,
                                     @PathVariable UUID id,
                                     @Valid @RequestBody UpdateTaskStatusRequest request) {
        return taskService.updateStatus(userDetails.getUsername(), id, request.status());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir tarefa do usuário autenticado")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID id) {
        taskService.delete(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
