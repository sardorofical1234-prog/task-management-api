package uz.portfolio.taskmanagement.project.exception;

public class ProjectOwnerRemovalException extends RuntimeException {

    public ProjectOwnerRemovalException() {
        super("The project owner cannot be removed from the project");
    }
}
