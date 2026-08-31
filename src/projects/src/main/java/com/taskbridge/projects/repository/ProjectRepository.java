package com.taskbridge.projects.repository;

import com.taskbridge.projects.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Find non-deleted projects for a tenant / team
     */
    Page<Project> findByTenantIdAndTeamIdAndDeletedFalse(String tenantId, String teamId, Pageable pageable);

    /**
     * Find by id and tenant (and not deleted).
     */
    Optional<Project> findByIdAndTenantIdAndDeletedFalse(Long id, String tenantId);
}
