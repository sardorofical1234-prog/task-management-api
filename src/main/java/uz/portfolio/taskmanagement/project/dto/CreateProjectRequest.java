package uz.portfolio.taskmanagement.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "Project name must not be blank")
        @Size(max = 120, message = "Project name must be at most 120 characters")
        String name,

        @Size(max = 2000, message = "Project description must be at most 2000 characters")
        String description
) {
}
