package com.harshqa.qadashboardai.controller;

import com.harshqa.qadashboardai.dto.FailurePatternDto;
import com.harshqa.qadashboardai.dto.FailureStatDto;
import com.harshqa.qadashboardai.dto.FlakyTestDto;
import com.harshqa.qadashboardai.dto.TrendDto;
import com.harshqa.qadashboardai.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/trends")
    public List<TrendDto> getTrends(@RequestParam(defaultValue = "7") int days) {
        return dashboardService.getTrendAnalysis(days);
    }

    @GetMapping("/top-failures")
    public List<FailureStatDto> getTopFailures(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "30") int days
    ) {
        return dashboardService.getTopFailures(limit, days);
    }

    @GetMapping("/failure-patterns")
    public List<FailurePatternDto> getFailurePatterns(@RequestParam(defaultValue = "30") int days) {
        return dashboardService.getFailurePatterns(days);
    }

    @GetMapping("/flaky-tests")
    public List<FlakyTestDto> getFlakyTests(@RequestParam(defaultValue = "30") int days) {
        return dashboardService.getFlakyTests(days);
    }

    // Endpoint to update Flaky Test Status
    @PostMapping("/flaky-tests/update")
    public FlakyTestDto updateFlakyTest(@RequestBody Map<String, Object> payload) {
        String className = (String) payload.get("className");
        String testName = (String) payload.get("testName");
        boolean acknowledged = (boolean) payload.get("acknowledged");
        String status = (String) payload.get("resolutionStatus");

        return dashboardService.updateFlakyStatus(className, testName, acknowledged, status);
    }
}