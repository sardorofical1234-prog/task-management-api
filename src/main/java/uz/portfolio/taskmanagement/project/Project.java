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
import uz.portfolio.taskmanagement.user.User;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "projects", indexes = @Index(name = "idx_projects_owner_created_at", columnList = "owner_id, created_at"))
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Project() {
        // Required by JPA.
    }

    public Project(String name, String description, User owner) {
        this.name = name;
        this.description = description;
        this.owner = Objects.requireNonNull(owner, "Project owner is required");
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public User getOwner() { return owner; }
    public Instant getCreatedAt() { return createdAt; }
}
