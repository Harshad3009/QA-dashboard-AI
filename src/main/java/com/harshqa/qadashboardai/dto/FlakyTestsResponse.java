package com.harshqa.qadashboardai.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class FlakyTestsResponse {
    private FlakyMetricsDto metrics;
    private List<FlakyTestDto> tests;
}