package com.harshqa.qadashboardai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity // <--- Tells JPA: "This class is a Database Table"
@Table(name = "test_runs") // Optional: Customizes the table name
public class TestRun {

    @Id // <--- Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // <--- Auto Increment (1, 2, 3...)
    private Long id;

    private LocalDateTime executionDate;
    private double totalDuration;

    private int totalTests;
    private int passCount;
    private int failCount;
    private int skipCount;

    // Store the JSON response from Gemini here.
    // It can be large, so we use @Lob.
    @Lob
    @Column(length = 10000)
    private String aiAnalysis;

    // Relationship: One TestRun has Many TestCases
    // "mappedBy" refers to the field name in the child class
    // CascadeType.ALL means: If I save the Run, save all its TestCases too.
    @OneToMany(mappedBy = "testRun", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TestCase> testCases = new ArrayList<>();

    // Link to Project
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore // Prevent infinite recursion in JSON response
    private Project project;

    // @Transient tells Hibernate/JPA: "Do not look for a 'status' column in the DB".
    // However, Jackson (JSON) will still call this getter and add "status" to the API response.
    @Transient
    public String getStatus() {
        if (totalTests == 0) {
            return "Unhealthy"; // Default to Unhealthy if empty
        }

        // Calculate Percentage: (Pass / Total) * 100
        double passRate = ((double) passCount / (totalTests-skipCount)) * 100;

        // Logic: > 85% is Healthy
        return passRate > 85 ? "Healthy" : "Unhealthy";
    }

}