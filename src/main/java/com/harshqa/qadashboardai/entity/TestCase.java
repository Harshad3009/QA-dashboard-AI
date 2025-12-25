package com.harshqa.qadashboardai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

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

    // Many TestCases can point to One TestFailure (The De-duplication)
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "failure_id")
    private TestFailure testFailure;

    // --- NEW HELPER FOR FRONTEND ---
    // This creates a JSON field "failureMessage" automatically!
    @JsonProperty("failureMessage")
    public String getFailureMessageText() {
        return testFailure != null ? testFailure.getStackTrace() : null;
    }

    // Relationship: Many TestCases belong to One TestRun
    @ManyToOne
    @JoinColumn(name = "run_id") // This creates the Foreign Key column in the DB
    @JsonIgnore
    @ToString.Exclude
    private TestRun testRun;
}