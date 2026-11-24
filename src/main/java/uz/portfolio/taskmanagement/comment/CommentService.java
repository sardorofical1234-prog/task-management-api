package uz.portfolio.taskmanagement.comment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.portfolio.taskmanagement.auth.security.CurrentUserService;
import uz.portfolio.taskmanagement.comment.dto.CommentResponse;
import uz.portfolio.taskmanagement.comment.dto.CreateCommentRequest;
import uz.portfolio.taskmanagement.task.Task;
import uz.portfolio.taskmanagement.task.TaskService;
import uz.portfolio.taskmanagement.user.User;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final TaskCommentRepository commentRepository;
    private final TaskService taskService;
    private final CurrentUserService currentUserService;

    public CommentService(
            TaskCommentRepository commentRepository,
            TaskService taskService,
            CurrentUserService currentUserService
    ) {
        this.commentRepository = commentRepository;
        this.taskService = taskService;
        this.currentUserService = currentUserService;
    }

    public List<CommentResponse> findByTask(Long taskId) {
        taskService.getAccessibleTask(taskId);
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId)
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse create(Long taskId, CreateCommentRequest request) {
        Task task = taskService.getAccessibleTask(taskId);
        User author = currentUserService.getCurrentUser();
        TaskComment comment = new TaskComment(task, author, request.content().trim());
        return CommentResponse.from(commentRepository.save(comment));
    }
}
