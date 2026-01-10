package com.harshqa.qadashboardai.service;

import com.harshqa.qadashboardai.dto.*;
import com.harshqa.qadashboardai.entity.*;
import com.harshqa.qadashboardai.repository.ProjectRepository;
import com.harshqa.qadashboardai.repository.TestCaseRepository;
import com.harshqa.qadashboardai.repository.TestManagementRepository;
import com.harshqa.qadashboardai.repository.TestRunRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TestRunRepository testRunRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestManagementRepository testManagementRepository;
    private final ProjectRepository projectRepository;

    public DashboardService(TestRunRepository testRunRepository,
                            TestCaseRepository testCaseRepository,
                            TestManagementRepository testManagementRepository, ProjectRepository projectRepository) {
        this.testRunRepository = testRunRepository;
        this.testCaseRepository = testCaseRepository;
        this.testManagementRepository = testManagementRepository;
        this.projectRepository = projectRepository;
    }

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
    }

    @Transactional(readOnly = true)
    public TrendsResponse getTrendAnalysis(int days, Long projectId) {
        Project project = getProject(projectId);
        // Calculate the cutoff date
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusDays(days);
        LocalDateTime prevCutoff = now.minusDays(days * 2L);

        // Fetch Current Period Runs for Project
        List<TestRun> runs = testRunRepository.findAllByProjectAndExecutionDateAfterOrderByExecutionDateDesc(project, cutoff);

        // Fetch Previous Period Runs for Project (For trend calculation)
        List<TestRun> prevRuns = testRunRepository.findAllByProjectAndExecutionDateBetween(project, prevCutoff, cutoff);

        // Calculate Daily Trends (The List)
        List<TrendDto> dailyTrends = calculateDailyTrends(runs);

        // 4. Calculate Top Level Metrics
        DashboardMetricsDto metrics = calculateDashboardMetrics(runs, prevRuns, cutoff, projectId);

        return TrendsResponse.builder()
                .metrics(metrics)
                .dailyTrends(dailyTrends)
                .build();
    }

    private List<TrendDto> calculateDailyTrends(List<TestRun> runs) {
        Map<LocalDate, List<TestRun>> groupedByDate = runs.stream()
                .collect(Collectors.groupingBy(run -> run.getExecutionDate().toLocalDate()));

        return groupedByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<TestRun> dailyRuns = entry.getValue();

                    int totalTests = dailyRuns.stream().mapToInt(TestRun::getTotalTests).sum();
                    int passCount = dailyRuns.stream().mapToInt(TestRun::getPassCount).sum();
                    int failCount = dailyRuns.stream().mapToInt(TestRun::getFailCount).sum();
                    int skipCount = dailyRuns.stream().mapToInt(TestRun::getSkipCount).sum();

                    // Execution times: Aggregate metrics from INDIVIDUAL TEST CASES, not the Suite Total
                    DoubleSummaryStatistics stats = dailyRuns.stream()
                            .flatMap(run -> run.getTestCases().stream()) // Drill down to test cases
                            .filter(tc -> !"SKIPPED".equalsIgnoreCase(tc.getStatus()))
                            .mapToDouble(TestCase::getDuration)          // Get individual durations
                            .summaryStatistics();

                    double passRate = totalTests > 0 ? (double) passCount / (totalTests-skipCount) * 100 : 0;

                    // Handle case where no tests exist to avoid Infinity/-Infinity
                    double avg = stats.getCount() > 0 ? stats.getAverage() : 0.0;
                    double max = stats.getCount() > 0 ? stats.getMax() : 0.0;
                    double min = stats.getCount() > 0 ? stats.getMin() : 0.0;

                    return TrendDto.builder()
                            .date(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .totalTests(totalTests)
                            .passCount(passCount)
                            .failCount(failCount)
                            .passRate(Math.round(passRate * 10.0) / 10.0)
                            .avgDuration(Math.round(avg * 100.0) / 100.0)
                            .maxDuration(Math.round(max * 100.0) / 100.0)
                            .minDuration(Math.round(min * 100.0) / 100.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private DashboardMetricsDto calculateDashboardMetrics(List<TestRun> currentRuns, List<TestRun> prevRuns, LocalDateTime cutoff, Long projectId) {
        // A. Total Runs
        int totalRuns = currentRuns.size();

        // B. Avg Pass Rate (Weighted by tests per run)
        long totalTests = currentRuns.stream().mapToLong(TestRun::getTotalTests).sum();
        long totalPass = currentRuns.stream().mapToLong(TestRun::getPassCount).sum();
        long totalFail = currentRuns.stream().mapToLong(TestRun::getFailCount).sum();
        long totalSkip = currentRuns.stream().mapToLong(TestRun::getSkipCount).sum();
        double avgPassRate = totalTests > 0 ? (double) totalPass / (totalTests-totalSkip) * 100 : 0.0;

        // C. Latest Pass Rate
        double latestPassRate = 0.0;
        if (!currentRuns.isEmpty()) {
            TestRun latest = currentRuns.getFirst(); // Already sorted DESC
            latestPassRate = latest.getTotalTests() > 0
                    ? (double) latest.getPassCount() / (latest.getTotalTests()-latest.getSkipCount()) * 100
                    : 0.0;
        }

        // D. Pass Rate Trend (Current vs Previous)
        long prevTotalTests = prevRuns.stream().mapToLong(TestRun::getTotalTests).sum();
        long prevTotalPass = prevRuns.stream().mapToLong(TestRun::getPassCount).sum();
        double prevPassRate = prevTotalTests > 0 ? (double) prevTotalPass / prevTotalTests * 100 : 0.0;

        double trend = avgPassRate - prevPassRate; // Positive = Good, Negative = Bad

        // E. Unique Failures
        long uniqueFailures = testCaseRepository.countUniqueFailures(cutoff, projectId);

        // F. Avg Execution Time (Across ALL tests in current period)
        DoubleSummaryStatistics execStats = currentRuns.stream()
                .flatMap(run -> run.getTestCases().stream())
                .filter(tc -> !"SKIPPED".equalsIgnoreCase(tc.getStatus()))
                .mapToDouble(TestCase::getDuration)
                .summaryStatistics();
        double avgExecTime = execStats.getCount() > 0 ? execStats.getAverage() : 0.0;

        return DashboardMetricsDto.builder()
                .totalRuns(totalRuns)
                .avgPassRate(Math.round(avgPassRate * 10.0) / 10.0)
                .latestPassRate(Math.round(latestPassRate * 10.0) / 10.0)
                .passRateTrend(Math.round(trend * 10.0) / 10.0)
                .totalUniqueFailures(uniqueFailures)
                .avgExecutionTime(Math.round(avgExecTime * 100.0) / 100.0)
                .build();
    }

    public List<FailureStatDto> getTopFailures(int limit, int days, Long projectId) {
        // Calculate Cutoff
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);

        // Ask repository for the top 'limit' items within cutoff period
        List<Object[]> results = testCaseRepository.findTopFailures(cutoff, projectId, PageRequest.of(0, limit));

        // Map the raw [Entity, Count] array to DTOs
        return results.stream()
                .map(row -> {
                    TestFailure failure = (TestFailure) row[0];
                    Long count = (Long) row[1];
                    return FailureStatDto.builder()
                            .failureId(failure.getId())
                            .hash(failure.getFailureHash())
                            .errorMessage(failure.getMessage()) // The short summary
                            .count(count)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public FlakyTestsResponse getFlakyTests(int days, int threshold, Long projectId) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        List<Object[]> results = testCaseRepository.findFlakyTests(cutoff, projectId);

        List<FlakyTestDto> tests = results.stream()
                .map(row -> {
                    String testName = (String) row[0];
                    String className = (String) row[1];
                    Long total = (Long) row[2];
                    Long failures = (Long) row[3];
                    double score = (double) failures / total * 100;

                    // Fetch management status (This could be optimized with a bulk fetch, but looping is fine for N < 100)
                    TestManagement mgmt = testManagementRepository
                            .findByClassNameAndTestName(className, testName)
                            .orElse(new TestManagement(className, testName));

                    return FlakyTestDto.builder()
                            .testName(testName)
                            .className(className)
                            .totalExecutions(total)
                            .failCount(failures)
                            .passCount(total - failures)
                            .flakinessScore(Math.round(score * 100.0) / 100.0)
                            // Management fields
                            .id(mgmt.getId() != null ? mgmt.getId().toString() : "new_" + className + "_" + testName)
                            .acknowledged(mgmt.isAcknowledged())
                            .resolutionStatus(mgmt.getResolutionStatus())
                            .build();
                })
                // Filter by Threshold
                .filter(t -> t.getFlakinessScore() >= threshold)
                // Sort Descending (Highest Flakiness First)
                .sorted((t1, t2) -> Double.compare(t2.getFlakinessScore(), t1.getFlakinessScore()))
                .collect(Collectors.toList());

        // Calculate Flaky Metrics
        int total = tests.size();
        int ack = (int) tests.stream().filter(FlakyTestDto::isAcknowledged).count();
        int resolved = (int) tests.stream().filter(t -> "resolved".equals(t.getResolutionStatus())).count();
        int progress = (int) tests.stream().filter(t -> "in-progress".equals(t.getResolutionStatus())).count();
        int investing = (int) tests.stream().filter(t -> "investigating".equals(t.getResolutionStatus())).count();
        int unresolved = (int) tests.stream().filter(t -> "unresolved".equals(t.getResolutionStatus())).count();

        FlakyMetricsDto metrics = FlakyMetricsDto.builder()
                .totalFlakyTests(total)
                .acknowledgedCount(ack)
                .resolvedCount(resolved)
                .inProgressCount(progress)
                .investigatingCount(investing)
                .unresolvedCount(unresolved)
                .build();

        return FlakyTestsResponse.builder()
                .metrics(metrics)
                .tests(tests)
                .build();
    }

    public FlakyTestDto updateFlakyStatus(String className, String testName, boolean acknowledged, String status) {
        TestManagement mgmt = testManagementRepository
                .findByClassNameAndTestName(className, testName)
                .orElse(new TestManagement(className, testName));

        mgmt.setAcknowledged(acknowledged);
        if (status != null) {
            mgmt.setResolutionStatus(status);
        }

        TestManagement saved = testManagementRepository.save(mgmt);

        return FlakyTestDto.builder()
                .testName(testName)
                .className(className)
                .acknowledged(saved.isAcknowledged())
                .resolutionStatus(saved.getResolutionStatus())
                .build();
    }

    public List<FailurePatternDto> getFailurePatterns(int days, Long projectId) {
        // Simple regex/keyword matching on top failures
        // In a real app, you might use AI or clustering here

        List<FailureStatDto> topFailures = getTopFailures(100, days, projectId);

        Map<String, Integer> categories = new HashMap<>();
        categories.put("Timeout", 0);
        categories.put("Assertion", 0);
        categories.put("Null Pointer", 0);
        categories.put("Stale Element", 0);
        categories.put("Connection", 0);
        categories.put("Other", 0);

        for (FailureStatDto fail : topFailures) {
            String msg = fail.getErrorMessage() != null ? fail.getErrorMessage().toLowerCase() : "";
            long count = fail.getCount();

            if (msg.contains("timeout") || msg.contains("nosuchelement")) {
                categories.merge("Timeout", (int)count, Integer::sum);
            } else if (msg.contains("assertion") || msg.contains("expected")) {
                categories.merge("Assertion", (int)count, Integer::sum);
            } else if (msg.contains("nullpointer")) {
                categories.merge("Null Pointer", (int)count, Integer::sum);
            } else if (msg.contains("stale") || msg.contains("detached")) {
                categories.merge("Stale Element", (int)count, Integer::sum);
            } else if (msg.contains("connection") || msg.contains("http")) {
                categories.merge("Connection", (int)count, Integer::sum);
            } else {
                categories.merge("Other", (int)count, Integer::sum);
            }
        }

        return categories.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> FailurePatternDto.builder()
                        .category(e.getKey())
                        .count(e.getValue())
                        .trend("stable") // Mock trend for now
                        .build())
                .sorted((a, b) -> b.getCount() - a.getCount())
                .collect(Collectors.toList());
    }
}