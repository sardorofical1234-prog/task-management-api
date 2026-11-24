package uz.portfolio.taskmanagement.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import uz.portfolio.taskmanagement.user.User;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "tasks",
        indexes = {
                @Index(name = "idx_tasks_status", columnList = "status"),
                @Index(name = "idx_tasks_owner_created_at", columnList = "owner_id, created_at")
        }
)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Nullable only to keep pre-ownership rows readable by ADMIN after migration.
    // Application-created tasks always receive an owner through the constructor.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    protected Task() {
        // Required by JPA.
    }

    public Task(String title, String description, User owner) {
        this.title = title;
        this.description = description;
        this.owner = Objects.requireNonNull(owner, "Task owner is required");
        this.status = TaskStatus.TODO;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = TaskStatus.TODO;
        }
    }

    public void update(String title, String description, TaskStatus status) {
        this.title = title;
        this.description = description;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public User getOwner() { return owner; }
}
