package com.harshqa.qadashboardai.service;

import com.harshqa.qadashboardai.dto.FailurePatternDto;
import com.harshqa.qadashboardai.dto.FailureStatDto;
import com.harshqa.qadashboardai.dto.FlakyTestDto;
import com.harshqa.qadashboardai.dto.TrendDto;
import com.harshqa.qadashboardai.entity.TestFailure;
import com.harshqa.qadashboardai.entity.TestManagement;
import com.harshqa.qadashboardai.entity.TestRun;
import com.harshqa.qadashboardai.repository.TestCaseRepository;
import com.harshqa.qadashboardai.repository.TestFailureRepository;
import com.harshqa.qadashboardai.repository.TestManagementRepository;
import com.harshqa.qadashboardai.repository.TestRunRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

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
    private final TestFailureRepository testFailureRepository;

    public DashboardService(TestRunRepository testRunRepository,
                            TestCaseRepository testCaseRepository,
                            TestManagementRepository testManagementRepository,
                            TestFailureRepository testFailureRepository) {
        this.testRunRepository = testRunRepository;
        this.testCaseRepository = testCaseRepository;
        this.testManagementRepository = testManagementRepository;
        this.testFailureRepository = testFailureRepository;
    }

    public List<TrendDto> getTrendAnalysis(int days) {
        // Calculate the cutoff date
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);

        // Fetch runs from DB
        List<TestRun> runs = testRunRepository.findAllByExecutionDateAfterOrderByExecutionDateDesc(cutoff);

        // Group by Date (LocalDate)
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

                    // Execution Times
                    DoubleSummaryStatistics stats = dailyRuns.stream()
                            .mapToDouble(TestRun::getTotalDuration)
                            .summaryStatistics();

                    double passRate = totalTests > 0 ? (double) passCount / totalTests * 100 : 0;

                    return TrendDto.builder()
                            .date(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .totalTests(totalTests)
                            .passCount(passCount)
                            .failCount(failCount)
                            .passRate(Math.round(passRate * 10.0) / 10.0)
                            .avgDuration(Math.round(stats.getAverage() * 100.0) / 100.0)
                            .maxDuration(Math.round(stats.getMax() * 100.0) / 100.0)
                            .minDuration(Math.round(stats.getMin() * 100.0) / 100.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<FailureStatDto> getTopFailures(int limit, int days) {
        // Calculate Cutoff
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);

        // Ask repository for the top 'limit' items within cutoff period
        List<Object[]> results = testCaseRepository.findTopFailures(cutoff, PageRequest.of(0, limit));

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

    public List<FlakyTestDto> getFlakyTests(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        List<Object[]> results = testCaseRepository.findFlakyTests(cutoff);

        return results.stream()
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
                .collect(Collectors.toList());
    }

    // New: Update Flaky Status
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

    // New: Failure Patterns
    public List<FailurePatternDto> getFailurePatterns(int days) {
        // Simple regex/keyword matching on top failures
        // In a real app, you might use AI or clustering here

        List<FailureStatDto> topFailures = getTopFailures(100, days);

        Map<String, Integer> categories = new HashMap<>();
        categories.put("Timeout", 0);
        categories.put("Assertion", 0);
        categories.put("Null Pointer", 0);
        categories.put("Stale Element", 0);
        categories.put("Connection", 0);
        categories.put("Other", 0);

        for (FailureStatDto fail : topFailures) {
            String msg = fail.getErrorMessage().toLowerCase();
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