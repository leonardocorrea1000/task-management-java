package br.dev.jcorrea.taskmanagement.task;

import br.dev.jcorrea.taskmanagement.exception.ResourceNotFoundException;
import br.dev.jcorrea.taskmanagement.task.dto.CreateTaskRequest;
import br.dev.jcorrea.taskmanagement.task.dto.TaskResponse;
import br.dev.jcorrea.taskmanagement.task.dto.UpdateTaskRequest;
import br.dev.jcorrea.taskmanagement.user.User;
import br.dev.jcorrea.taskmanagement.user.UserRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> list(String userEmail, TaskStatus status, Pageable pageable) {
        User user = getCurrentUser(userEmail);
        Page<Task> tasks = status == null
                ? taskRepository.findByUserId(user.getId(), pageable)
                : taskRepository.findByUserIdAndStatus(user.getId(), status, pageable);
        return tasks.map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(String userEmail, UUID taskId) {
        User user = getCurrentUser(userEmail);
        return TaskResponse.from(findOwnedTask(taskId, user.getId()));
    }

    @Transactional
    public TaskResponse create(String userEmail, CreateTaskRequest request) {
        User user = getCurrentUser(userEmail);
        Task task = new Task(
                request.title().trim(),
                request.description(),
                request.status() == null ? TaskStatus.PENDING : request.status(),
                request.dueDate(),
                user
        );
        Task saved = taskRepository.save(task);
        log.info("Tarefa criada: {}", saved.getId());
        return TaskResponse.from(saved);
    }

    @Transactional
    public TaskResponse update(String userEmail, UUID taskId, UpdateTaskRequest request) {
        User user = getCurrentUser(userEmail);
        Task task = findOwnedTask(taskId, user.getId());
        task.update(request.title().trim(), request.description(), request.status(), request.dueDate());
        log.info("Tarefa atualizada: {}", task.getId());
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse updateStatus(String userEmail, UUID taskId, TaskStatus status) {
        User user = getCurrentUser(userEmail);
        Task task = findOwnedTask(taskId, user.getId());
        task.updateStatus(status);
        log.info("Status de tarefa atualizado: {}", task.getId());
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(String userEmail, UUID taskId) {
        User user = getCurrentUser(userEmail);
        Task task = findOwnedTask(taskId, user.getId());
        taskRepository.delete(task);
        log.info("Tarefa excluída: {}", task.getId());
    }

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private Task findOwnedTask(UUID taskId, UUID userId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada"));
    }
}
