package com.taskbridge.projects.dto;

import com.taskbridge.projects.model.ProjectStatus;

import java.time.Instant;

/**
 * Response DTO returned to clients. Does NOT expose tenantId.
 */
public class ProjectResponse {
    private Long id;
    private String name;
    private String teamId;
    private ProjectStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    // Getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
