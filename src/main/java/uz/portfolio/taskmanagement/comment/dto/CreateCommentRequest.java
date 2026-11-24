package uz.portfolio.taskmanagement.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Comment content must not be blank")
        @Size(max = 2000, message = "Comment content must be at most 2000 characters")
        String content
) {
}
