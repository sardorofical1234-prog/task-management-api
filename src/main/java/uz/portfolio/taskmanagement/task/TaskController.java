package uz.portfolio.taskmanagement.task;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.portfolio.taskmanagement.task.dto.CreateTaskRequest;
import uz.portfolio.taskmanagement.task.dto.PageResponse;
import uz.portfolio.taskmanagement.task.dto.TaskResponse;
import uz.portfolio.taskmanagement.task.dto.UpdateTaskRequest;
import uz.portfolio.taskmanagement.task.dto.UpdateTaskStatusRequest;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks", description = "Create, search and manage tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @Operation(summary = "Create a task")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse created = taskService.create(request);
        URI location = URI.create("/api/v1/tasks/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "Get tasks with pagination, sorting and filtering")
    public PageResponse<TaskResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String search
    ) {
        return taskService.findAll(page, size, sortBy, direction, status, search);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task by ID")
    public TaskResponse findById(@PathVariable Long id) {
        return taskService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a task's editable fields")
    public TaskResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return taskService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change only a task's status")
    public TaskResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        return taskService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
