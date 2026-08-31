package com.taskbridge.projects.dto;

import com.taskbridge.projects.model.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload to create a Project.
 * tenantId is passed as X-Tenant-Id header in controllers.
 */
public class CreateProjectRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String teamId;

    @NotNull
    private ProjectStatus status;

    // Optional actor (user id) performing the action
    private String createdBy;

    // Getters & setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
