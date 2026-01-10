package com.harshqa.qadashboardai.config;

import com.harshqa.qadashboardai.entity.Project;
import com.harshqa.qadashboardai.repository.ProjectRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ProjectRepository projectRepository;

    public DataSeeder(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (projectRepository.count() == 0) {
            System.out.println("Seeding Default Projects...");
            projectRepository.save(new Project("Colo Android", "Mobile Automation for Colo Android App"));
            projectRepository.save(new Project("Colo iOS", "Mobile Automation Colo for iOS App"));
            projectRepository.save(new Project("Colo Backend", "API Automation Suite for Tracks Backend"));
        }
    }
}