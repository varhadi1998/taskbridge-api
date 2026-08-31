// NOTE: AI-generated, unreviewed (copied directly from Copilot output)
// Generated from prompt: "Generate a Project model and a Project service with create, update status, get by team, and delete functions. Use a database."
package com.taskbridge.projects.service;

import com.taskbridge.projects.model.Project;
import com.taskbridge.projects.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    private final ProjectRepository repo;

    public ProjectService(ProjectRepository repo) {
        this.repo = repo;
    }

    public Project createProject(Project p) {
        if (p.getStatus() == null) p.setStatus("new");
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return repo.save(p);
    }

    public Project updateStatus(Long projectId, String status) {
        Optional<Project> opt = repo.findById(projectId);
        if (!opt.isPresent()) {
            throw new RuntimeException("Project not found");
        }
        Project p = opt.get();
        p.setStatus(status);
        p.setUpdatedAt(Instant.now());
        return repo.save(p);
    }

    public List<Project> getByTeam(String teamId) {
        return repo.findByTeamId(teamId);
    }

    public void deleteProject(Long projectId) {
        repo.deleteById(projectId);
    }
}
