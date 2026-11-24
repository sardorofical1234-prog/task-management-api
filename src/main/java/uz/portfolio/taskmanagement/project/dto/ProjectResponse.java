package uz.portfolio.taskmanagement.project.dto;

import uz.portfolio.taskmanagement.project.Project;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        Long ownerId,
        Instant createdAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getOwner().getId(),
                project.getCreatedAt()
        );
    }
}
