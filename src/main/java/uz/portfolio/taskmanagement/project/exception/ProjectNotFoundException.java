package uz.portfolio.taskmanagement.project.exception;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(Long id) {
        super("Project not found with id: " + id);
    }
}
