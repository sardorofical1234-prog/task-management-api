package uz.portfolio.taskmanagement.project;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.portfolio.taskmanagement.project.dto.AddProjectMemberRequest;
import uz.portfolio.taskmanagement.project.dto.CreateProjectRequest;
import uz.portfolio.taskmanagement.project.dto.ProjectMemberResponse;
import uz.portfolio.taskmanagement.project.dto.ProjectResponse;
import uz.portfolio.taskmanagement.project.dto.UpdateProjectRequest;
import uz.portfolio.taskmanagement.task.TaskService;
import uz.portfolio.taskmanagement.task.dto.CreateTaskRequest;
import uz.portfolio.taskmanagement.task.dto.TaskResponse;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "Create projects and manage project members")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;

    public ProjectController(ProjectService projectService, TaskService taskService) {
        this.projectService = projectService;
        this.taskService = taskService;
    }

    @PostMapping
    @Operation(summary = "Create a project")
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse created = projectService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/projects/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List projects available to the current user")
    public List<ProjectResponse> findAll() {
        return projectService.findAll();
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get a project")
    public ProjectResponse findById(@PathVariable Long projectId) {
        return projectService.findById(projectId);
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "Update a project (owner or ADMIN)")
    public ProjectResponse update(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return projectService.update(projectId, request);
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Delete a project (owner or ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Long projectId) {
        projectService.delete(projectId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{projectId}/members")
    @Operation(summary = "Add a user to a project (owner or ADMIN)")
    public ResponseEntity<ProjectMemberResponse> addMember(
            @PathVariable Long projectId,
            @Valid @RequestBody AddProjectMemberRequest request
    ) {
        return ResponseEntity.status(201).body(projectService.addMember(projectId, request));
    }

    @GetMapping("/{projectId}/members")
    @Operation(summary = "List project members")
    public List<ProjectMemberResponse> findMembers(@PathVariable Long projectId) {
        return projectService.findMembers(projectId);
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    @Operation(summary = "Remove a project member (owner or ADMIN)")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long userId
    ) {
        projectService.removeMember(projectId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{projectId}/tasks")
    @Operation(summary = "Create a task inside a project")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        TaskResponse created = taskService.createInProject(projectId, request);
        return ResponseEntity.created(URI.create("/api/v1/tasks/" + created.id())).body(created);
    }
}
