package com.harshqa.qadashboardai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrendDto {
    private String date;      // e.g., "2025-12-20"
    private int totalTests;
    private int passCount;
    private int failCount;
    private double passRate;  // e.g., 95.5
}