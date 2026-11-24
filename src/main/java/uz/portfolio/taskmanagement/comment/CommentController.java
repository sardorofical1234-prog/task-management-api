package uz.portfolio.taskmanagement.comment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.portfolio.taskmanagement.comment.dto.CommentResponse;
import uz.portfolio.taskmanagement.comment.dto.CreateCommentRequest;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/comments")
@Tag(name = "Task comments", description = "Discuss tasks with project members")
@SecurityRequirement(name = "bearerAuth")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @Operation(summary = "Add a comment to an accessible task")
    public ResponseEntity<CommentResponse> create(
            @PathVariable Long taskId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        CommentResponse created = commentService.create(taskId, request);
        return ResponseEntity.created(
                URI.create("/api/v1/tasks/" + taskId + "/comments/" + created.id())
        ).body(created);
    }

    @GetMapping
    @Operation(summary = "List comments for an accessible task")
    public List<CommentResponse> findByTask(@PathVariable Long taskId) {
        return commentService.findByTask(taskId);
    }
}
