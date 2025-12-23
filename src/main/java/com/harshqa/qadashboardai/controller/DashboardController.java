package com.harshqa.qadashboardai.controller;

import com.harshqa.qadashboardai.dto.FailureStatDto;
import com.harshqa.qadashboardai.dto.TrendDto;
import com.harshqa.qadashboardai.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // Endpoint: GET /api/dashboard/trends?days=7
    @GetMapping("/trends")
    public List<TrendDto> getTrends(@RequestParam(defaultValue = "7") int days) {
        return dashboardService.getTrendAnalysis(days);
    }

    // Endpoint: GET /api/dashboard/top-failures?limit=5
    @GetMapping("/top-failures")
    public List<FailureStatDto> getTopFailures(@RequestParam(defaultValue = "5") int limit) {
        return dashboardService.getTopFailures(limit);
    }
}