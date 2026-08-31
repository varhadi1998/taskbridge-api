package com.taskbridge.projects.controller;

import com.taskbridge.projects.dto.CreateProjectRequest;
import com.taskbridge.projects.dto.ProjectResponse;
import com.taskbridge.projects.dto.UpdateStatusRequest;
import com.taskbridge.projects.service.ProjectService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * ProjectController exposes thin endpoints and delegates business logic to ProjectService.
 *
 * Security note: Authentication/authorization should be enforced at API Gateway. Controllers must
 * additionally validate X-Tenant-Id matches caller's tenant claim (optional double-check).
 */
@RestController
@RequestMapping("/api/projects")
@Validated
public class ProjectController {

    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectService svc;

    public ProjectController(ProjectService svc) {
        this.svc = svc;
    }

    /**
     * Create a project. X-Tenant-Id header is required.
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody @Valid CreateProjectRequest req) {

        log.info("Create project request tenant={} team={} name={}", tenantId, req.getTeamId(), req.getName());
        ProjectResponse created = svc.createProject(tenantId, req);
        return ResponseEntity.ok(created);
    }

    /**
     * Update project status.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProjectResponse> updateStatus(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable("id") Long projectId,
            @RequestBody @Valid UpdateStatusRequest req) {

        ProjectResponse updated = svc.updateStatus(tenantId, projectId, req);
        return ResponseEntity.ok(updated);
    }

    /**
     * Get projects by team (paginated).
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<Page<ProjectResponse>> byTeam(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ProjectResponse> results = svc.getByTeam(tenantId, teamId, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Soft-delete project.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable("id") Long projectId,
            @RequestParam(required = false) String deletedBy) {

        svc.deleteProject(tenantId, projectId, deletedBy);
        return ResponseEntity.noContent().build();
    }
}
