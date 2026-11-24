package uz.portfolio.taskmanagement.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uz.portfolio.taskmanagement.task.dto.PageResponse;
import uz.portfolio.taskmanagement.task.dto.TaskResponse;
import uz.portfolio.taskmanagement.task.dto.UpdateTaskStatusRequest;
import uz.portfolio.taskmanagement.user.User;
import uz.portfolio.taskmanagement.user.UserRepository;
import uz.portfolio.taskmanagement.user.UserRole;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void userRepository_shouldPersistUserWithDefaultRole() {
        User user = userRepository.saveAndFlush(
                new User("developer@example.com", "bcrypt-hash-placeholder")
        );

        assertThat(user.getId()).isNotNull();
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(userRepository.findByEmail("developer@example.com"))
                .isPresent()
                .get()
                .extracting(User::getPasswordHash)
                .isEqualTo("bcrypt-hash-placeholder");
    }

    @Test
    void findAll_shouldFilterSortAndPaginateTasks() {
        taskRepository.save(new Task("Design API", "Define REST endpoints"));
        taskRepository.save(new Task("Design UI", "Prepare wireframes"));

        Task completedTask = new Task("Design backend", "Implement persistence");
        completedTask.update(
                completedTask.getTitle(),
                completedTask.getDescription(),
                TaskStatus.DONE
        );
        taskRepository.save(completedTask);

        ResponseEntity<PageResponse<TaskResponse>> response = restTemplate.exchange(
                "/api/v1/tasks?page=0&size=1&sortBy=title&direction=asc&status=TODO&search=design",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content())
                .extracting(TaskResponse::title)
                .containsExactly("Design API");
        assertThat(response.getBody().page()).isZero();
        assertThat(response.getBody().size()).isEqualTo(1);
        assertThat(response.getBody().totalElements()).isEqualTo(2);
        assertThat(response.getBody().totalPages()).isEqualTo(2);
        assertThat(response.getBody().first()).isTrue();
        assertThat(response.getBody().last()).isFalse();
    }

    @Test
    void updateStatus_shouldChangeOnlyTheTaskStatus() {
        Task task = taskRepository.saveAndFlush(
                new Task("Prepare documentation", "Write API usage guide")
        );

        ResponseEntity<TaskResponse> response = restTemplate.exchange(
                "/api/v1/tasks/{id}/status",
                HttpMethod.PATCH,
                new HttpEntity<>(new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS)),
                TaskResponse.class,
                task.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.getBody().title()).isEqualTo("Prepare documentation");
        assertThat(response.getBody().description()).isEqualTo("Write API usage guide");
    }

    @Test
    void findAll_shouldRejectUnsupportedSortField() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/tasks?sortBy=password",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("sortBy");
    }

    @Test
    void create_shouldRejectBlankTitle() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/tasks",
                new HttpEntity<>(Map.of("title", " ", "description", "Invalid task"), headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("title");
    }
}
