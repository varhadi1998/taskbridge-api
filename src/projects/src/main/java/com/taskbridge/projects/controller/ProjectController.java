package com.taskbridge.projects.controller;

import com.taskbridge.projects.model.Project;
import com.taskbridge.projects.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService svc;

    public ProjectController(ProjectService svc) {
        this.svc = svc;
    }

    @PostMapping
    public ResponseEntity<Project> create(@RequestBody Project p) {
        Project created = svc.createProject(p);
        return ResponseEntity.ok(created);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Project> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Project updated = svc.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<Project>> byTeam(@PathVariable String teamId) {
        return ResponseEntity.ok(svc.getByTeam(teamId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        svc.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
