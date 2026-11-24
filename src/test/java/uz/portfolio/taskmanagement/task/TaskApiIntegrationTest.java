package uz.portfolio.taskmanagement.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskApiIntegrationTest {

    private static final String TEST_PASSWORD = "Password123!";

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

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    void register_shouldStoreBcryptHashAndDefaultUserRole() {
        String email = uniqueEmail();

        ResponseEntity<Map> response = register(email, TEST_PASSWORD);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsEntry("email", email);
        assertThat(response.getBody()).containsEntry("role", "USER");

        User savedUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(TEST_PASSWORD);
        assertThat(passwordEncoder.matches(TEST_PASSWORD, savedUser.getPasswordHash())).isTrue();
    }

    @Test
    void register_shouldRejectDuplicateEmail() {
        String email = uniqueEmail();

        assertThat(register(email, TEST_PASSWORD).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(register(email, TEST_PASSWORD).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void login_shouldIssueJwtThatCanAccessTaskEndpoints() {
        String email = uniqueEmail();
        register(email, TEST_PASSWORD);

        ResponseEntity<Map> loginResponse = login(email, TEST_PASSWORD);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).containsEntry("tokenType", "Bearer");
        assertThat(loginResponse.getBody().get("accessToken")).isInstanceOf(String.class);
        assertThat(loginResponse.getBody().get("expiresIn")).isEqualTo(900);

        String token = (String) loginResponse.getBody().get("accessToken");
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> tasksResponse = restTemplate.exchange(
                "/api/v1/tasks",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(tasksResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void login_shouldRejectWrongPassword() {
        String email = uniqueEmail();
        register(email, TEST_PASSWORD);

        assertThat(login(email, "WrongPassword123!").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void taskEndpoints_shouldRejectRequestsWithoutJwt() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/tasks",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
                new HttpEntity<>(authenticatedHeaders()),
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
                new HttpEntity<>(
                        new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS),
                        authenticatedHeaders()
                ),
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
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/tasks?sortBy=password",
                HttpMethod.GET,
                new HttpEntity<>(authenticatedHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("sortBy");
    }

    @Test
    void create_shouldRejectBlankTitle() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/tasks",
                new HttpEntity<>(
                        Map.of("title", " ", "description", "Invalid task"),
                        authenticatedHeaders()
                ),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("title");
    }

    private ResponseEntity<Map> register(String email, String password) {
        return restTemplate.postForEntity(
                "/api/v1/auth/register",
                new HttpEntity<>(
                        Map.of("email", email, "password", password),
                        jsonHeaders()
                ),
                Map.class
        );
    }

    private ResponseEntity<Map> login(String email, String password) {
        return restTemplate.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(
                        Map.of("email", email, "password", password),
                        jsonHeaders()
                ),
                Map.class
        );
    }

    private HttpHeaders authenticatedHeaders() {
        String email = uniqueEmail();
        assertThat(register(email, TEST_PASSWORD).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map> loginResponse = login(email, TEST_PASSWORD);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth((String) loginResponse.getBody().get("accessToken"));
        return headers;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
