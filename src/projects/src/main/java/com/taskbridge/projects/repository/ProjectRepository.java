package com.taskbridge.projects.repository;

import com.taskbridge.projects.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByTeamId(String teamId);
}
