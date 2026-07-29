package br.dev.jcorrea.taskmanagement.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.dev.jcorrea.taskmanagement.exception.ResourceNotFoundException;
import br.dev.jcorrea.taskmanagement.task.dto.CreateTaskRequest;
import br.dev.jcorrea.taskmanagement.task.dto.UpdateTaskRequest;
import br.dev.jcorrea.taskmanagement.user.User;
import br.dev.jcorrea.taskmanagement.user.UserRepository;
import br.dev.jcorrea.taskmanagement.user.UserRole;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    private TaskService taskService;
    private User owner;
    private Task task;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, userRepository);
        owner = new User("Leo", "leo@example.com", "hash", UserRole.USER);
        task = new Task("Título", "Descrição", TaskStatus.PENDING, LocalDate.now().plusDays(1), owner);
    }

    @Test
    void createsTaskForAuthenticatedUser() {
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = taskService.create(owner.getEmail(), new CreateTaskRequest(" Nova ", null, null, null));

        assertThat(response.title()).isEqualTo("Nova");
        assertThat(response.status()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void listsOnlyOwnTasks() {
        var pageable = PageRequest.of(0, 20);
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(taskRepository.findByUserId(owner.getId(), pageable)).thenReturn(new PageImpl<>(List.of(task)));

        var page = taskService.list(owner.getEmail(), null, pageable);

        assertThat(page.getContent()).hasSize(1);
        verify(taskRepository).findByUserId(owner.getId(), pageable);
    }

    @Test
    void getsOwnTaskAndHidesOtherUsersTaskAsNotFound() {
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(taskRepository.findByIdAndUserId(task.getId(), owner.getId())).thenReturn(Optional.of(task));

        assertThat(taskService.getById(owner.getEmail(), task.getId()).id()).isEqualTo(task.getId());

        when(taskRepository.findByIdAndUserId(task.getId(), owner.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> taskService.getById(owner.getEmail(), task.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tarefa não encontrada");
    }

    @Test
    void updatesTaskAndStatusAndDeletes() {
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(taskRepository.findByIdAndUserId(task.getId(), owner.getId())).thenReturn(Optional.of(task));

        var updated = taskService.update(owner.getEmail(), task.getId(),
                new UpdateTaskRequest("Atualizada", "Nova descrição", TaskStatus.IN_PROGRESS, null));
        var status = taskService.updateStatus(owner.getEmail(), task.getId(), TaskStatus.COMPLETED);
        taskService.delete(owner.getEmail(), task.getId());

        assertThat(updated.title()).isEqualTo("Atualizada");
        assertThat(status.status()).isEqualTo(TaskStatus.COMPLETED);
        verify(taskRepository).delete(task);
    }
}
