package com.harshqa.qadashboardai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FlakyTestDto {
    private String testName;
    private String className;
    private long totalExecutions;
    private long failCount;
    private double flakinessScore; // e.g., 50% (Failed half the time)
}