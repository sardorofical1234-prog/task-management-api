package uz.portfolio.taskmanagement.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.portfolio.taskmanagement.task.dto.CreateTaskRequest;
import uz.portfolio.taskmanagement.task.dto.PageResponse;
import uz.portfolio.taskmanagement.task.dto.TaskResponse;
import uz.portfolio.taskmanagement.task.dto.UpdateTaskRequest;
import uz.portfolio.taskmanagement.task.exception.InvalidTaskQueryException;
import uz.portfolio.taskmanagement.task.exception.TaskNotFoundException;

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

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        Task task = new Task(request.title().trim(), request.description());
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

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        String normalizedSearch = search == null || search.isBlank()
                ? null
                : search.trim().toLowerCase(Locale.ROOT);

        Specification<Task> specification = (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();

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
        return TaskResponse.from(getTaskOrThrow(id));
    }

    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        Task task = getTaskOrThrow(id);
        task.update(request.title().trim(), request.description(), request.status());
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateStatus(Long id, TaskStatus status) {
        Task task = getTaskOrThrow(id);
        task.update(task.getTitle(), task.getDescription(), status);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        taskRepository.delete(getTaskOrThrow(id));
    }

    private Task getTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }
}
