package uz.portfolio.taskmanagement.comment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import uz.portfolio.taskmanagement.task.Task;
import uz.portfolio.taskmanagement.user.User;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "task_comments", indexes = @Index(name = "idx_task_comments_task_created", columnList = "task_id, created_at"))
public class TaskComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TaskComment() {
        // Required by JPA.
    }

    public TaskComment(Task task, User author, String content) {
        this.task = Objects.requireNonNull(task);
        this.author = Objects.requireNonNull(author);
        this.content = Objects.requireNonNull(content);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public Task getTask() { return task; }
    public User getAuthor() { return author; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
