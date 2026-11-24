package uz.portfolio.taskmanagement.task;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.portfolio.taskmanagement.auth.security.CurrentUserService;
import uz.portfolio.taskmanagement.project.Project;
import uz.portfolio.taskmanagement.project.ProjectMember;
import uz.portfolio.taskmanagement.project.ProjectMemberRepository;
import uz.portfolio.taskmanagement.project.ProjectService;
import uz.portfolio.taskmanagement.task.dto.CreateTaskRequest;
import uz.portfolio.taskmanagement.task.dto.PageResponse;
import uz.portfolio.taskmanagement.task.dto.TaskAssignmentRequest;
import uz.portfolio.taskmanagement.task.dto.TaskResponse;
import uz.portfolio.taskmanagement.task.dto.UpdateTaskRequest;
import uz.portfolio.taskmanagement.task.exception.InvalidTaskQueryException;
import uz.portfolio.taskmanagement.task.exception.TaskNotFoundException;
import uz.portfolio.taskmanagement.user.User;
import uz.portfolio.taskmanagement.user.UserRepository;
import uz.portfolio.taskmanagement.user.exception.UserNotFoundException;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "title", "status", "createdAt");

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectService projectService,
            CurrentUserService currentUserService
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectService = projectService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        User creator = currentUserService.getCurrentUser();
        Task task = new Task(request.title().trim(), request.description(), creator);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse createInProject(Long projectId, CreateTaskRequest request) {
        Project project = projectService.getAccessibleProject(projectId);
        User creator = currentUserService.getCurrentUser();
        Task task = new Task(request.title().trim(), request.description(), creator, project);
        return TaskResponse.from(taskRepository.save(task));
    }

    public PageResponse<TaskResponse> findAll(
            int page,
            int size,
            String sortBy,
            String direction,
            TaskStatus status,
            String search
    ) {
        if (page < 0) {
            throw new InvalidTaskQueryException("Page index must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidTaskQueryException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new InvalidTaskQueryException(
                    "sortBy must be one of: " + String.join(", ", ALLOWED_SORT_FIELDS)
            );
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new InvalidTaskQueryException("direction must be either 'asc' or 'desc'");
        }

        boolean admin = currentUserService.isAdmin();
        User currentUser = admin ? null : currentUserService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        String normalizedSearch = search == null || search.isBlank()
                ? null
                : search.trim().toLowerCase(Locale.ROOT);

        Specification<Task> specification = (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();

            if (!admin) {
                Join<Task, User> creator = root.join("owner", JoinType.LEFT);
                Join<Task, User> assignee = root.join("assignedTo", JoinType.LEFT);
                Join<Task, Project> project = root.join("project", JoinType.LEFT);
                Join<Project, User> projectOwner = project.join("owner", JoinType.LEFT);

                Subquery<Long> membershipQuery = query.subquery(Long.class);
                Root<ProjectMember> membership = membershipQuery.from(ProjectMember.class);
                membershipQuery.select(membership.get("id")).where(
                        criteriaBuilder.equal(membership.get("project").get("id"), project.get("id")),
                        criteriaBuilder.equal(membership.get("user").get("id"), currentUser.getId())
                );

                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(creator.get("id"), currentUser.getId()),
                        criteriaBuilder.equal(assignee.get("id"), currentUser.getId()),
                        criteriaBuilder.equal(projectOwner.get("id"), currentUser.getId()),
                        criteriaBuilder.exists(membershipQuery)
                ));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (normalizedSearch != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")),
                        "%" + normalizedSearch + "%"
                ));
            }

            return criteriaBuilder.and(predicates.toArray(
                    jakarta.persistence.criteria.Predicate[]::new
            ));
        };

        Page<Task> tasks = taskRepository.findAll(specification, pageable);
        return PageResponse.from(tasks, TaskResponse::from);
    }

    public TaskResponse findById(Long id) {
        return TaskResponse.from(getAccessibleTask(id));
    }

    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        Task task = getAccessibleTask(id);
        task.update(request.title().trim(), request.description(), request.status());
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateStatus(Long id, TaskStatus status) {
        Task task = getAccessibleTask(id);
        task.update(task.getTitle(), task.getDescription(), status);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse assign(Long taskId, TaskAssignmentRequest request) {
        Task task = getAccessibleTask(taskId);
        Project project = projectService.getManageableProject(request.projectId());

        if (task.getProject() != null
                && !currentUserService.isAdmin()
                && !task.getProject().getOwner().getId().equals(currentUserService.getCurrentUser().getId())) {
            throw new TaskNotFoundException(taskId);
        }

        User assignee = userRepository.findById(request.assigneeId())
                .orElseThrow(() -> new UserNotFoundException(request.assigneeId()));

        if (!projectService.isOwnerOrMember(project, assignee)) {
            throw new InvalidTaskQueryException("The assignee must be the project owner or a project member");
        }

        task.assign(project, assignee);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        taskRepository.delete(getAccessibleTask(id));
    }

    public Task getAccessibleTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        if (currentUserService.isAdmin()) {
            return task;
        }

        User currentUser = currentUserService.getCurrentUser();
        boolean createdByUser = task.getOwner() != null
                && task.getOwner().getId().equals(currentUser.getId());
        boolean assignedToUser = task.getAssignedTo() != null
                && task.getAssignedTo().getId().equals(currentUser.getId());
        boolean projectOwner = task.getProject() != null
                && task.getProject().getOwner().getId().equals(currentUser.getId());
        boolean projectMember = task.getProject() != null
                && projectMemberRepository.existsByProjectIdAndUserId(
                        task.getProject().getId(),
                        currentUser.getId()
                );

        if (createdByUser || assignedToUser || projectOwner || projectMember) {
            return task;
        }

        throw new TaskNotFoundException(id);
    }
}
