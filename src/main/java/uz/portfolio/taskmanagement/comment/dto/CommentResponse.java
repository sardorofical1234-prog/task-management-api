package uz.portfolio.taskmanagement.comment.dto;

import uz.portfolio.taskmanagement.comment.TaskComment;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long taskId,
        Long authorId,
        String authorEmail,
        String content,
        Instant createdAt
) {
    public static CommentResponse from(TaskComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTask().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getEmail(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
