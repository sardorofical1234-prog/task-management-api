package uz.portfolio.taskmanagement.auth.dto;

import uz.portfolio.taskmanagement.user.UserRole;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        UserRole role,
        Instant createdAt
) {
}
