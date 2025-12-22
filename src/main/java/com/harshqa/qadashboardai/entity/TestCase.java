package com.harshqa.qadashboardai.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "test_cases")
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String testName;
    private String className;
    private double duration;

    // Storing Status as a String for simplicity (PASS, FAIL, SKIP)
    private String status;

    // Use @Lob for Large Objects (Long text like stack traces)
    @Lob
    @Column(length = 10000) // Allow up to 10k chars
    private String failureMessage;

    // Relationship: Many TestCases belong to One TestRun
    @ManyToOne
    @JoinColumn(name = "run_id") // This creates the Foreign Key column in the DB
    private TestRun testRun;
}