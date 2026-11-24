package uz.portfolio.taskmanagement.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.portfolio.taskmanagement.auth.security.CurrentUserService;
import uz.portfolio.taskmanagement.project.ProjectMemberRepository;
import uz.portfolio.taskmanagement.project.ProjectService;
import uz.portfolio.taskmanagement.task.dto.CreateTaskRequest;
import uz.portfolio.taskmanagement.task.dto.TaskResponse;
import uz.portfolio.taskmanagement.user.User;
import uz.portfolio.taskmanagement.user.UserRepository;

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

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private TaskService taskService;

    @Test
    void create_shouldSaveTaskWithTodoStatusAndAuthenticatedOwner() {
        User owner = new User("developer@example.com", "bcrypt-hash");
        when(currentUserService.getCurrentUser()).thenReturn(owner);
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
        assertThat(response.projectId()).isNull();
        assertThat(response.assigneeId()).isNull();
    }
}
