package uz.portfolio.taskmanagement.task.dto;

import uz.portfolio.taskmanagement.task.Task;
import uz.portfolio.taskmanagement.task.TaskStatus;

import java.time.Instant;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Instant createdAt,
        Long projectId,
        Long assigneeId
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getProject() == null ? null : task.getProject().getId(),
                task.getAssignedTo() == null ? null : task.getAssignedTo().getId()
        );
    }
}
