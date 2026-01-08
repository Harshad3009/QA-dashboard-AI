package com.harshqa.qadashboardai.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TrendsResponse {
    private DashboardMetricsDto metrics;
    private List<TrendDto> dailyTrends;
}