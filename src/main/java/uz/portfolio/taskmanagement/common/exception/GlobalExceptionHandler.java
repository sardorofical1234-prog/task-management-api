package uz.portfolio.taskmanagement.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import uz.portfolio.taskmanagement.auth.exception.EmailAlreadyRegisteredException;
import uz.portfolio.taskmanagement.auth.exception.InvalidCredentialsException;
import uz.portfolio.taskmanagement.auth.exception.InvalidPasswordException;
import uz.portfolio.taskmanagement.project.exception.ProjectMemberAlreadyExistsException;
import uz.portfolio.taskmanagement.project.exception.ProjectMemberNotFoundException;
import uz.portfolio.taskmanagement.project.exception.ProjectNotFoundException;
import uz.portfolio.taskmanagement.project.exception.ProjectOwnerRemovalException;
import uz.portfolio.taskmanagement.task.exception.InvalidTaskQueryException;
import uz.portfolio.taskmanagement.task.exception.TaskNotFoundException;
import uz.portfolio.taskmanagement.user.exception.UserNotFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            TaskNotFoundException.class,
            ProjectNotFoundException.class,
            ProjectMemberNotFoundException.class,
            UserNotFoundException.class
    })
    public ResponseEntity<ProblemDetail> handleNotFound(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
        problem.setTitle("Resource not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ProblemDetail> handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException exception
    ) {
        return conflict(exception.getMessage());
    }

    @ExceptionHandler(ProjectMemberAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleProjectMemberAlreadyExists(
            ProjectMemberAlreadyExistsException exception
    ) {
        return conflict(exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(
            InvalidCredentialsException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage()
        );
        problem.setTitle("Authentication failed");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ProblemDetail> handleInvalidPassword(
            InvalidPasswordException exception
    ) {
        return badRequest(exception.getMessage());
    }

    @ExceptionHandler(ProjectOwnerRemovalException.class)
    public ResponseEntity<ProblemDetail> handleProjectOwnerRemoval(
            ProjectOwnerRemovalException exception
    ) {
        return badRequest(exception.getMessage());
    }

    @ExceptionHandler(InvalidTaskQueryException.class)
    public ResponseEntity<ProblemDetail> handleInvalidTaskQuery(InvalidTaskQueryException exception) {
        return badRequest(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        return badRequest(
                "Parameter '%s' has invalid value '%s'".formatted(
                        exception.getName(),
                        exception.getValue()
                )
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return badRequest("Request body is invalid or contains an unsupported value");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        String details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return badRequest(details);
    }

    private ResponseEntity<ProblemDetail> conflict(String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        problem.setTitle("Conflict");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    private ResponseEntity<ProblemDetail> badRequest(String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Invalid request");
        return ResponseEntity.badRequest().body(problem);
    }
}
