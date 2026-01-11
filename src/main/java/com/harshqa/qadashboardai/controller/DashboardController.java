package com.harshqa.qadashboardai.controller;

import com.harshqa.qadashboardai.dto.*;
import com.harshqa.qadashboardai.service.DashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public TrendsResponse getTrends(@RequestParam(defaultValue = "7") int days,
                                    @RequestParam Long projectId) {
        return dashboardService.getTrendAnalysis(days, projectId);
    }

    @GetMapping("/top-failures")
    public List<FailureStatDto> getTopFailures(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam Long projectId
    ) {
        return dashboardService.getTopFailures(limit, days, projectId);
    }

    @GetMapping("/failure-patterns")
    public List<FailurePatternDto> getFailurePatterns(@RequestParam(defaultValue = "30") int days,
                                                      @RequestParam Long projectId) {
        return dashboardService.getFailurePatterns(days, projectId);
    }

    @GetMapping("/flaky-tests")
    public FlakyTestsResponse getFlakyTests(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "0") int flakyThreshold,
            @RequestParam Long projectId) {
        return dashboardService.getFlakyTests(days, flakyThreshold, projectId);
    }

    // Endpoint to update Flaky Test Status
    @PostMapping("/flaky-tests/update")
    public FlakyTestDto updateFlakyTest(@RequestBody Map<String, Object> payload) {
        String className = (String) payload.get("className");
        String testName = (String) payload.get("testName");
        boolean acknowledged = (boolean) payload.get("acknowledged");
        String status = (String) payload.get("resolutionStatus");
        String assignee = (String) payload.get("assignee");

        // --- RBAC CHECK ---
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        // If user is trying to change assignee
        if (assignee != null) {
            boolean isManager = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
            // Rule: Engineers can only assign to themselves
            if (!isManager && !assignee.equals(currentUsername)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Engineers can only assign tests to themselves.");
            }
        }

        return dashboardService.updateFlakyStatus(className, testName, acknowledged, status, assignee);
    }
}