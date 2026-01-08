package com.harshqa.qadashboardai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardMetricsDto {
    private int totalRuns;
    private double avgPassRate;
    private double latestPassRate;
    private double passRateTrend; // The +/- difference vs previous period
    private long totalUniqueFailures; // Unique test cases failed in this period
    private double avgExecutionTime; // In seconds
}