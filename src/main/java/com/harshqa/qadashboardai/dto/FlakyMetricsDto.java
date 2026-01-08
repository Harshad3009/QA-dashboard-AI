package com.harshqa.qadashboardai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FlakyMetricsDto {
    private int totalFlakyTests;
    private int acknowledgedCount;
    private int inProgressCount;
    private int resolvedCount;
    private int investigatingCount;
    private int unresolvedCount;
}