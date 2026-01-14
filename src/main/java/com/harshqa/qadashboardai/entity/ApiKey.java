package com.harshqa.qadashboardai.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_key", nullable = false, unique = true)
    private String secretKey;

    private String name; // e.g., "Github - Android"

    @ManyToOne(fetch = FetchType.EAGER) // Eager needed for security filter
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;

    public ApiKey(String name, Project project) {
        this.name = name;
        this.project = project;
        this.secretKey = "qad_" + UUID.randomUUID().toString().replace("-", "");
        this.createdAt = LocalDateTime.now();
    }
}