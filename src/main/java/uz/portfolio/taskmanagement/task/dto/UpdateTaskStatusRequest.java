package uz.portfolio.taskmanagement.task.dto;

import jakarta.validation.constraints.NotNull;
import uz.portfolio.taskmanagement.task.TaskStatus;

public record UpdateTaskStatusRequest(
        @NotNull(message = "Task status is required")
        TaskStatus status
) {
}
