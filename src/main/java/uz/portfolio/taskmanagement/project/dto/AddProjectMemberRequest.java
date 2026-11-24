package uz.portfolio.taskmanagement.project.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddProjectMemberRequest(
        @NotNull
        @Positive
        Long userId
) {
}
