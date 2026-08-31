// NOTE: AI-generated, unreviewed (copied directly from Copilot output)
// Generated from prompt: "Generate a Project model and a Project service with create, update status, get by team, and delete functions. Use a database."
package com.taskbridge.projects.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String teamId;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public Project() {}

    public Project(String name, String teamId, String status) {
        this.name = name;
        this.teamId = teamId;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
