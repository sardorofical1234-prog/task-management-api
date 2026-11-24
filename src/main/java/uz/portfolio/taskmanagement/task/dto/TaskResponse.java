package uz.portfolio.taskmanagement.task.dto;

import uz.portfolio.taskmanagement.task.Task;
import uz.portfolio.taskmanagement.task.TaskStatus;

import java.time.Instant;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Instant createdAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(),
                task.getStatus(), task.getCreatedAt());
    }
}
