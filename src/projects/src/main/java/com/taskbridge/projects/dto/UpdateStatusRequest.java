package com.taskbridge.projects.dto;

import com.taskbridge.projects.model.ProjectStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload to update project status.
 */
public class UpdateStatusRequest {

    @NotNull
    private ProjectStatus status;

    private String changedBy;

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
}
