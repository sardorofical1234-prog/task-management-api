package uz.portfolio.taskmanagement.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank(message = "Task title must not be blank")
        @Size(max = 120, message = "Task title must be at most 120 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description
) {
}
