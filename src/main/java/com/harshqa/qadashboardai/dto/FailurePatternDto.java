package com.harshqa.qadashboardai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FailurePatternDto {
    private String category; // e.g., "Timeout", "Assertion"
    private int count;
    private String trend;    // "up", "down", "stable" (Simple calculation)
}