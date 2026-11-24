package uz.portfolio.taskmanagement.project.dto;

import uz.portfolio.taskmanagement.project.ProjectMember;
import uz.portfolio.taskmanagement.user.UserRole;

import java.time.Instant;

public record ProjectMemberResponse(
        Long userId,
        String email,
        UserRole role,
        Instant joinedAt
) {
    public static ProjectMemberResponse from(ProjectMember member) {
        return new ProjectMemberResponse(
                member.getUser().getId(),
                member.getUser().getEmail(),
                member.getUser().getRole(),
                member.getJoinedAt()
        );
    }
}
