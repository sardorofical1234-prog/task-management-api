package uz.portfolio.taskmanagement.task.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TaskAssignmentRequest(
        @NotNull
        @Positive
        Long projectId,

        @NotNull
        @Positive
        Long assigneeId
) {
}
