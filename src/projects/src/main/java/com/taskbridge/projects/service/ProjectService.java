package com.taskbridge.projects.service;

import com.taskbridge.projects.dto.CreateProjectRequest;
import com.taskbridge.projects.dto.ProjectResponse;
import com.taskbridge.projects.dto.UpdateStatusRequest;
import com.taskbridge.projects.exception.NotFoundException;
import com.taskbridge.projects.model.Project;
import com.taskbridge.projects.model.ProjectStatus;
import com.taskbridge.projects.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer encapsulating project business logic.
 *
 * All public methods require tenantId which must be enforced at the controller layer.
 */
@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository repo;
    private final AuditPublisher auditPublisher;

    public ProjectService(ProjectRepository repo, AuditPublisher auditPublisher) {
        this.repo = repo;
        this.auditPublisher = auditPublisher;
    }

    /**
     * Create a new project in the tenant scope.
     *
     * @param tenantId tenant id (from X-Tenant-Id header)
     * @param req creation request (validated)
     * @return ProjectResponse DTO
     */
    @Transactional
    public ProjectResponse createProject(String tenantId, CreateProjectRequest req) {
        Project p = new Project();
        p.setTenantId(tenantId);
        p.setName(req.getName());
        p.setTeamId(req.getTeamId());
        p.setStatus(Optional.ofNullable(req.getStatus()).orElse(ProjectStatus.NEW));
        p.setCreatedBy(req.getCreatedBy());
        Project saved = repo.save(p);

        // Publish audit event (best-effort; publisher may write outbox atomically)
        try {
            auditPublisher.publishAudit(tenantId, saved.getId(), "PROJECT_CREATED", req.getCreatedBy(), Map.of(
                    "name", req.getName(),
                    "teamId", req.getTeamId(),
                    "status", saved.getStatus().name()
            ));
        } catch (Exception ex) {
            // Don't fail creation on publisher error; surface metric/logging instead
            log.error("Failed to publish audit for project {}: {}", saved.getId(), ex.getMessage(), ex);
        }

        return mapToResponse(saved);
    }

    /**
     * Update the status of a project within tenant scope.
     *
     * @param tenantId tenant id
     * @param projectId project id
     * @param req update request
     * @return updated ProjectResponse
     */
    @Transactional
    public ProjectResponse updateStatus(String tenantId, Long projectId, UpdateStatusRequest req) {
        var opt = repo.findByIdAndTenantIdAndDeletedFalse(projectId, tenantId);
        var project = opt.orElseThrow(() -> new NotFoundException("Project not found"));

        ProjectStatus oldStatus = project.getStatus();
        project.setStatus(req.getStatus());
        Project updated = repo.save(project);

        // Publish audit change
        try {
            auditPublisher.publishAudit(tenantId, updated.getId(), "PROJECT_STATUS_UPDATED", req.getChangedBy(),
                    Map.of("status", Map.of("old", oldStatus.name(), "new", req.getStatus().name())));
        } catch (Exception ex) {
            log.error("Failed to publish audit for project status update {}: {}", updated.getId(), ex.getMessage(), ex);
        }

        return mapToResponse(updated);
    }

    /**
     * Fetch projects by team within a tenant, paginated.
     */
    @Transactional(readOnly = true)
    public Page<ProjectResponse> getByTeam(String tenantId, String teamId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Project> projects = repo.findByTenantIdAndTeamIdAndDeletedFalse(tenantId, teamId, pageable);
        return new PageImpl<>(
                projects.stream().map(this::mapToResponse).collect(Collectors.toList()),
                pageable,
                projects.getTotalElements()
        );
    }

    /**
     * Soft-delete project (mark deleted=true). Idempotent operation.
     */
    @Transactional
    public void deleteProject(String tenantId, Long projectId, String deletedBy) {
        var opt = repo.findByIdAndTenantIdAndDeletedFalse(projectId, tenantId);
        var project = opt.orElseThrow(() -> new NotFoundException("Project not found"));
        project.setDeleted(true);
        repo.save(project);

        try {
            auditPublisher.publishAudit(tenantId, projectId, "PROJECT_DELETED", deletedBy, Map.of("deleted", true));
        } catch (Exception ex) {
            log.error("Failed to publish audit for project delete {}: {}", projectId, ex.getMessage(), ex);
        }
    }

    // Utility mapping method
    private ProjectResponse mapToResponse(Project p) {
        ProjectResponse r = new ProjectResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setTeamId(p.getTeamId());
        r.setStatus(p.getStatus());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }
}
