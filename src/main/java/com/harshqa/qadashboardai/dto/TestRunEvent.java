package com.harshqa.qadashboardai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestRunEvent {
    private Long runId;
    private String status; // Healthy/Unhealthy
    private Long projectId;
    private String projectName;
    private int totalTests;
    private int failCount;
    private String type; // "NEW_RUN" or "UPDATE"
}