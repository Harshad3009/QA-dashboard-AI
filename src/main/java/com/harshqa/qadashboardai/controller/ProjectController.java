package com.harshqa.qadashboardai.controller;

import com.harshqa.qadashboardai.entity.ApiKey;
import com.harshqa.qadashboardai.entity.Project;
import com.harshqa.qadashboardai.repository.ApiKeyRepository;
import com.harshqa.qadashboardai.repository.ProjectRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final ApiKeyRepository apiKeyRepository;

    public ProjectController(ProjectRepository projectRepository, ApiKeyRepository apiKeyRepository) {
        this.projectRepository = projectRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    @GetMapping
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    // --- API KEY MANAGEMENT ---

    @GetMapping("/{projectId}/keys")
    @PreAuthorize("hasRole('MANAGER')") // Only Managers can see keys
    public List<ApiKey> getProjectKeys(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        return apiKeyRepository.findByProject(project);
    }

    @PostMapping("/{projectId}/keys")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiKey generateKey(@PathVariable Long projectId, @RequestBody Map<String, String> payload) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        String name = payload.getOrDefault("name", "CI Token");
        ApiKey newKey = new ApiKey(name, project);
        return apiKeyRepository.save(newKey);
    }

    @DeleteMapping("/keys/{keyId}")
    @PreAuthorize("hasRole('MANAGER')")
    public void revokeKey(@PathVariable Long keyId) {
        apiKeyRepository.deleteById(keyId);
    }
}
