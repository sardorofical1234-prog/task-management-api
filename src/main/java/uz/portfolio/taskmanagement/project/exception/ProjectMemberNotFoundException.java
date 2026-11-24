package uz.portfolio.taskmanagement.project.exception;

public class ProjectMemberNotFoundException extends RuntimeException {

    public ProjectMemberNotFoundException(Long userId, Long projectId) {
        super("User %d is not a member of project %d".formatted(userId, projectId));
    }
}
