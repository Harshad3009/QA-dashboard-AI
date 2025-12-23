package com.harshqa.qadashboardai.entity;

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
}