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
import uz.portfolio.taskmanagement.comment.TaskCommentRepository;
import uz.portfolio.taskmanagement.comment.dto.CommentResponse;
import uz.portfolio.taskmanagement.comment.dto.CreateCommentRequest;
import uz.portfolio.taskmanagement.project.ProjectMemberRepository;
import uz.portfolio.taskmanagement.project.ProjectRepository;
import uz.portfolio.taskmanagement.project.dto.AddProjectMemberRequest;
import uz.portfolio.taskmanagement.project.dto.CreateProjectRequest;
import uz.portfolio.taskmanagement.project.dto.ProjectResponse;
import uz.portfolio.taskmanagement.task.dto.CreateTaskRequest;
import uz.portfolio.taskmanagement.task.dto.PageResponse;
import uz.portfolio.taskmanagement.task.dto.TaskAssignmentRequest;
import uz.portfolio.taskmanagement.task.dto.TaskResponse;
import uz.portfolio.taskmanagement.task.dto.UpdateTaskRequest;
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

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private TaskCommentRepository commentRepository;

    @BeforeEach
    void cleanDatabase() {
        commentRepository.deleteAll();
        taskRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void actuatorHealthEndpoints_shouldBePublicAndHideDetails() {
        ResponseEntity<String> readiness = restTemplate.getForEntity(
                "/actuator/health/readiness",
                String.class
        );
        ResponseEntity<String> liveness = restTemplate.getForEntity(
                "/actuator/health/liveness",
                String.class
        );

        assertThat(readiness.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readiness.getBody()).contains("\\"status\\":\\"UP\\"");
        assertThat(readiness.getBody()).doesNotContain("components");
        assertThat(liveness.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(liveness.getBody()).contains("\\"status\\":\\"UP\\"");
        assertThat(liveness.getBody()).doesNotContain("components");
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

        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth((String) loginResponse.getBody().get("accessToken"));

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
    void user_shouldOnlyListOwnTasks() {
        TestAccount alice = createAccount();
        TestAccount bob = createAccount();

        createTask(alice, "Alice private task", "Only Alice should see this");
        createTask(bob, "Bob private task", "Only Bob should see this");

        ResponseEntity<PageResponse<TaskResponse>> response = restTemplate.exchange(
                "/api/v1/tasks",
                HttpMethod.GET,
                new HttpEntity<>(alice.headers()),
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content())
                .extracting(TaskResponse::title)
                .containsExactly("Alice private task");
        assertThat(response.getBody().totalElements()).isEqualTo(1);
    }

    @Test
    void user_shouldNotReadUpdateOrDeleteAnotherUsersTask() {
        TestAccount alice = createAccount();
        TestAccount bob = createAccount();
        TaskResponse alicesTask = createTask(alice, "Private task", "Alice's task");

        ResponseEntity<String> readResponse = restTemplate.exchange(
                "/api/v1/tasks/{id}",
                HttpMethod.GET,
                new HttpEntity<>(bob.headers()),
                String.class,
                alicesTask.id()
        );
        ResponseEntity<String> updateResponse = restTemplate.exchange(
                "/api/v1/tasks/{id}",
                HttpMethod.PUT,
                new HttpEntity<>(
                        new UpdateTaskRequest("Hijacked task", "Changed by Bob", TaskStatus.DONE),
                        bob.headers()
                ),
                String.class,
                alicesTask.id()
        );
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/api/v1/tasks/{id}",
                HttpMethod.DELETE,
                new HttpEntity<>(bob.headers()),
                String.class,
                alicesTask.id()
        );

        assertThat(readResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        Task persistedTask = taskRepository.findById(alicesTask.id()).orElseThrow();
        assertThat(persistedTask.getTitle()).isEqualTo("Private task");
        assertThat(persistedTask.getOwner().getId()).isEqualTo(alice.user().getId());
    }

    @Test
    void admin_shouldReadAndUpdateAnotherUsersTask() {
        TestAccount alice = createAccount();
        TestAccount admin = createAdminAccount();
        TaskResponse alicesTask = createTask(alice, "Alice task", "Created by Alice");

        ResponseEntity<TaskResponse> readResponse = restTemplate.exchange(
                "/api/v1/tasks/{id}",
                HttpMethod.GET,
                new HttpEntity<>(admin.headers()),
                TaskResponse.class,
                alicesTask.id()
        );
        ResponseEntity<TaskResponse> updateResponse = restTemplate.exchange(
                "/api/v1/tasks/{id}",
                HttpMethod.PUT,
                new HttpEntity<>(
                        new UpdateTaskRequest("Admin updated task", "Updated by admin", TaskStatus.DONE),
                        admin.headers()
                ),
                TaskResponse.class,
                alicesTask.id()
        );

        assertThat(readResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readResponse.getBody().title()).isEqualTo("Alice task");
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().title()).isEqualTo("Admin updated task");
        assertThat(taskRepository.findById(alicesTask.id()).orElseThrow().getOwner().getId())
                .isEqualTo(alice.user().getId());
    }

    @Test
    void findAll_shouldFilterSortAndPaginateTasksForCurrentUser() {
        TestAccount account = createAccount();
        TestAccount anotherUser = createAccount();

        taskRepository.save(new Task("Design API", "Define REST endpoints", account.user()));
        taskRepository.save(new Task("Design UI", "Prepare wireframes", account.user()));

        Task completedTask = new Task("Design backend", "Implement persistence", account.user());
        completedTask.update(
                completedTask.getTitle(),
                completedTask.getDescription(),
                TaskStatus.DONE
        );
        taskRepository.save(completedTask);
        taskRepository.save(new Task("Design secret", "Belongs to another user", anotherUser.user()));

        ResponseEntity<PageResponse<TaskResponse>> response = restTemplate.exchange(
                "/api/v1/tasks?page=0&size=1&sortBy=title&direction=asc&status=TODO&search=design",
                HttpMethod.GET,
                new HttpEntity<>(account.headers()),
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
    void admin_shouldListTasksFromAllUsers() {
        TestAccount alice = createAccount();
        TestAccount bob = createAccount();
        TestAccount admin = createAdminAccount();
        createTask(alice, "Alice task", "Alice");
        createTask(bob, "Bob task", "Bob");

        ResponseEntity<PageResponse<TaskResponse>> response = restTemplate.exchange(
                "/api/v1/tasks",
                HttpMethod.GET,
                new HttpEntity<>(admin.headers()),
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content())
                .extracting(TaskResponse::title)
                .containsExactlyInAnyOrder("Alice task", "Bob task");
    }

    @Test
    void updateStatus_shouldChangeOnlyTheTaskStatus() {
        TestAccount account = createAccount();
        TaskResponse created = createTask(account, "Prepare documentation", "Write API usage guide");

        ResponseEntity<TaskResponse> response = restTemplate.exchange(
                "/api/v1/tasks/{id}/status",
                HttpMethod.PATCH,
                new HttpEntity<>(
                        new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS),
                        account.headers()
                ),
                TaskResponse.class,
                created.id()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.getBody().title()).isEqualTo("Prepare documentation");
        assertThat(response.getBody().description()).isEqualTo("Write API usage guide");
    }

    @Test
    void findAll_shouldRejectUnsupportedSortField() {
        TestAccount account = createAccount();
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/tasks?sortBy=password",
                HttpMethod.GET,
                new HttpEntity<>(account.headers()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("sortBy");
    }

    @Test
    void create_shouldRejectBlankTitle() {
        TestAccount account = createAccount();
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/tasks",
                new HttpEntity<>(
                        Map.of("title", " ", "description", "Invalid task"),
                        account.headers()
                ),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("title");
    }


    @Test
    void projectOwner_shouldManageProjectAndMembersWhileMembersCanOnlyView() {
        TestAccount owner = createAccount();
        TestAccount member = createAccount();
        TestAccount outsider = createAccount();
        ProjectResponse project = createProject(owner, "Backend Portfolio");

        addMember(owner, project.id(), member.user().getId());

        ResponseEntity<ProjectResponse> memberRead = restTemplate.exchange(
                "/api/v1/projects/{projectId}",
                HttpMethod.GET,
                new HttpEntity<>(member.headers()),
                ProjectResponse.class,
                project.id()
        );
        ResponseEntity<String> outsiderRead = restTemplate.exchange(
                "/api/v1/projects/{projectId}",
                HttpMethod.GET,
                new HttpEntity<>(outsider.headers()),
                String.class,
                project.id()
        );
        ResponseEntity<String> memberUpdate = restTemplate.exchange(
                "/api/v1/projects/{projectId}",
                HttpMethod.PUT,
                new HttpEntity<>(
                        Map.of("name", "Unauthorized rename", "description", "Should be rejected"),
                        member.headers()
                ),
                String.class,
                project.id()
        );

        assertThat(memberRead.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(memberRead.getBody().name()).isEqualTo("Backend Portfolio");
        assertThat(outsiderRead.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(memberUpdate.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> memberList = restTemplate.exchange(
                "/api/v1/projects",
                HttpMethod.GET,
                new HttpEntity<>(member.headers()),
                String.class
        );
        assertThat(memberList.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(memberList.getBody()).contains("Backend Portfolio");
    }

    @Test
    void projectOwner_shouldAssignTaskToProjectMemberAndAssigneeCanAccessIt() {
        TestAccount owner = createAccount();
        TestAccount member = createAccount();
        TestAccount outsider = createAccount();
        ProjectResponse project = createProject(owner, "Payment Service");
        addMember(owner, project.id(), member.user().getId());

        ResponseEntity<TaskResponse> createResponse = restTemplate.postForEntity(
                "/api/v1/projects/{projectId}/tasks",
                new HttpEntity<>(
                        new CreateTaskRequest("Implement payment API", "Create payment endpoints"),
                        member.headers()
                ),
                TaskResponse.class,
                project.id()
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TaskResponse createdTask = createResponse.getBody();
        assertThat(createdTask.projectId()).isEqualTo(project.id());

        ResponseEntity<TaskResponse> assignmentResponse = restTemplate.exchange(
                "/api/v1/tasks/{taskId}/assignment",
                HttpMethod.PATCH,
                new HttpEntity<>(
                        new TaskAssignmentRequest(project.id(), member.user().getId()),
                        owner.headers()
                ),
                TaskResponse.class,
                createdTask.id()
        );

        assertThat(assignmentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(assignmentResponse.getBody().assigneeId()).isEqualTo(member.user().getId());

        ResponseEntity<TaskResponse> assigneeRead = restTemplate.exchange(
                "/api/v1/tasks/{taskId}",
                HttpMethod.GET,
                new HttpEntity<>(member.headers()),
                TaskResponse.class,
                createdTask.id()
        );
        ResponseEntity<String> outsiderRead = restTemplate.exchange(
                "/api/v1/tasks/{taskId}",
                HttpMethod.GET,
                new HttpEntity<>(outsider.headers()),
                String.class,
                createdTask.id()
        );

        assertThat(assigneeRead.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(outsiderRead.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> invalidAssignment = restTemplate.exchange(
                "/api/v1/tasks/{taskId}/assignment",
                HttpMethod.PATCH,
                new HttpEntity<>(
                        new TaskAssignmentRequest(project.id(), outsider.user().getId()),
                        owner.headers()
                ),
                String.class,
                createdTask.id()
        );
        assertThat(invalidAssignment.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void projectMember_shouldCommentOnTaskAndUnrelatedUserCannotReadComments() {
        TestAccount owner = createAccount();
        TestAccount member = createAccount();
        TestAccount outsider = createAccount();
        ProjectResponse project = createProject(owner, "User Service");
        addMember(owner, project.id(), member.user().getId());

        TaskResponse task = createTaskInProject(owner, project.id(), "Add user endpoint", "Create profile API");

        ResponseEntity<CommentResponse> commentResponse = restTemplate.postForEntity(
                "/api/v1/tasks/{taskId}/comments",
                new HttpEntity<>(
                        new CreateCommentRequest("I have started the implementation."),
                        member.headers()
                ),
                CommentResponse.class,
                task.id()
        );

        assertThat(commentResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(commentResponse.getBody().authorEmail()).isEqualTo(member.user().getEmail());

        ResponseEntity<CommentResponse[]> ownerComments = restTemplate.exchange(
                "/api/v1/tasks/{taskId}/comments",
                HttpMethod.GET,
                new HttpEntity<>(owner.headers()),
                CommentResponse[].class,
                task.id()
        );
        ResponseEntity<String> outsiderComments = restTemplate.exchange(
                "/api/v1/tasks/{taskId}/comments",
                HttpMethod.GET,
                new HttpEntity<>(outsider.headers()),
                String.class,
                task.id()
        );

        assertThat(ownerComments.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ownerComments.getBody()).hasSize(1);
        assertThat(ownerComments.getBody()[0].content())
                .isEqualTo("I have started the implementation.");
        assertThat(outsiderComments.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<Map> register(String email, String password) {
        return restTemplate.postForEntity(
                "/api/v1/auth/register",
                new HttpEntity<>(Map.of("email", email, "password", password), jsonHeaders()),
                Map.class
        );
    }

    private ResponseEntity<Map> login(String email, String password) {
        return restTemplate.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(Map.of("email", email, "password", password), jsonHeaders()),
                Map.class
        );
    }

    private TestAccount createAccount() {
        String email = uniqueEmail();
        assertThat(register(email, TEST_PASSWORD).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return authenticatedAccount(email);
    }

    private TestAccount createAdminAccount() {
        String email = uniqueEmail();
        User admin = userRepository.saveAndFlush(
                new User(email, passwordEncoder.encode(TEST_PASSWORD), UserRole.ADMIN)
        );
        return authenticatedAccount(admin.getEmail());
    }

    private TestAccount authenticatedAccount(String email) {
        ResponseEntity<Map> loginResponse = login(email, TEST_PASSWORD);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth((String) loginResponse.getBody().get("accessToken"));
        User user = userRepository.findByEmail(email).orElseThrow();
        return new TestAccount(user, headers);
    }

    private TaskResponse createTask(TestAccount account, String title, String description) {
        ResponseEntity<TaskResponse> response = restTemplate.postForEntity(
                "/api/v1/tasks",
                new HttpEntity<>(new CreateTaskRequest(title, description), account.headers()),
                TaskResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }


    private ProjectResponse createProject(TestAccount owner, String name) {
        ResponseEntity<ProjectResponse> response = restTemplate.postForEntity(
                "/api/v1/projects",
                new HttpEntity<>(
                        new CreateProjectRequest(name, "Integration test project"),
                        owner.headers()
                ),
                ProjectResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private void addMember(TestAccount owner, Long projectId, Long userId) {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/projects/{projectId}/members",
                new HttpEntity<>(new AddProjectMemberRequest(userId), owner.headers()),
                String.class,
                projectId
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private TaskResponse createTaskInProject(
            TestAccount creator,
            Long projectId,
            String title,
            String description
    ) {
        ResponseEntity<TaskResponse> response = restTemplate.postForEntity(
                "/api/v1/projects/{projectId}/tasks",
                new HttpEntity<>(
                        new CreateTaskRequest(title, description),
                        creator.headers()
                ),
                TaskResponse.class,
                projectId
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private record TestAccount(User user, HttpHeaders headers) {
    }
}
