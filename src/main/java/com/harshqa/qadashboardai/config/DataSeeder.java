package com.harshqa.qadashboardai.config;

import com.harshqa.qadashboardai.entity.Project;
import com.harshqa.qadashboardai.repository.ProjectRepository;
import com.harshqa.qadashboardai.entity.User;
import com.harshqa.qadashboardai.entity.Role;
import com.harshqa.qadashboardai.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(ProjectRepository projectRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (projectRepository.count() == 0) {
            System.out.println("Seeding Default Projects...");
            projectRepository.save(new Project("Mobile Android", "Mobile Automation for Android App"));
            projectRepository.save(new Project("Mobile iOS", "Mobile Automation for iOS App"));
            projectRepository.save(new Project("Backend API", "API Automation Suite for Backend"));
            System.out.println("Default Projects Created.");
        }
        if (userRepository.count() == 0) {
            System.out.println("Seeding Default Users...");
            userRepository.save(new User("manager", passwordEncoder.encode("password"), Role.MANAGER));
            userRepository.save(new User("dev", passwordEncoder.encode("password"), Role.ENGINEER));
            userRepository.save(new User("tester", passwordEncoder.encode("password"), Role.ENGINEER));
            System.out.println("Users Created: manager/password, dev/password");
        }
    }
}