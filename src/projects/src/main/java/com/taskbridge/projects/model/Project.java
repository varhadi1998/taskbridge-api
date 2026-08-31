package com.taskbridge.projects.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * Project entity with tenant scoping and soft-delete support.
 *
 * Notes:
 * - tenantId must be provided on all writes and reads.
 * - deleted flag implements soft-delete semantics.
 */
@Entity
@Table(name = "projects",
       indexes = {
           @Index(name = "idx_projects_tenant_team", columnList = "tenant_id, team_id"),
           @Index(name = "idx_projects_tenant_created", columnList = "tenant_id, created_at")
       })
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(name = "team_id", nullable = false)
    private String teamId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "created_by", nullable = true)
    private String createdBy;

    public Project() {}

    public Project(String tenantId, String name, String teamId, ProjectStatus status, String createdBy) {
        this.tenantId = tenantId;
        this.name = name;
        this.teamId = teamId;
        this.status = status;
        this.createdBy = createdBy;
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters / setters
    public Long getId() { return id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project)) return false;
        Project project = (Project) o;
        return Objects.equals(id, project.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
