package com.harshqa.qadashboardai.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@Table(name = "test_management", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"className", "testName"})
})
public class TestManagement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String className;

    @Column(nullable = false)
    private String testName;

    private boolean acknowledged;

    // unresolved, investigating, in-progress, resolved
    private String resolutionStatus = "unresolved";

    private String assignee;
    private String notes;

    public TestManagement(String className, String testName) {
        this.className = className;
        this.testName = testName;
    }
}