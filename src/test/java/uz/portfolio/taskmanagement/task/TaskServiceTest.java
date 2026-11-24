package uz.portfolio.taskmanagement.task;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import uz.portfolio.taskmanagement.task.dto.CreateTaskRequest;
import uz.portfolio.taskmanagement.task.dto.TaskResponse;
import uz.portfolio.taskmanagement.user.User;
import uz.portfolio.taskmanagement.user.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_shouldSaveTaskWithTodoStatusAndAuthenticatedOwner() {
        String email = "developer@example.com";
        User owner = new User(email, "bcrypt-hash");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.create(
                new CreateTaskRequest("Learn Java", "Practice Spring Boot")
        );

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());

        assertThat(taskCaptor.getValue().getTitle()).isEqualTo("Learn Java");
        assertThat(taskCaptor.getValue().getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(taskCaptor.getValue().getOwner()).isSameAs(owner);
        assertThat(response.title()).isEqualTo("Learn Java");
    }
}
