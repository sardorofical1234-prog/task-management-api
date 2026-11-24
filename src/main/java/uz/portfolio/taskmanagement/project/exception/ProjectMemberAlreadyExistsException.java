package uz.portfolio.taskmanagement.project.exception;

public class ProjectMemberAlreadyExistsException extends RuntimeException {

    public ProjectMemberAlreadyExistsException(Long userId, Long projectId) {
        super("User %d is already a member of project %d".formatted(userId, projectId));
    }
}
