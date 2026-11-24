package uz.portfolio.taskmanagement.project;

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
import jakarta.persistence.UniqueConstraint;
import uz.portfolio.taskmanagement.user.User;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "project_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_project_members_project_user",
                columnNames = {"project_id", "user_id"}
        ),
        indexes = @Index(name = "idx_project_members_user", columnList = "user_id")
)
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected ProjectMember() {
        // Required by JPA.
    }

    public ProjectMember(Project project, User user) {
        this.project = Objects.requireNonNull(project);
        this.user = Objects.requireNonNull(user);
    }

    @PrePersist
    void onCreate() {
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public User getUser() { return user; }
    public Instant getJoinedAt() { return joinedAt; }
}
