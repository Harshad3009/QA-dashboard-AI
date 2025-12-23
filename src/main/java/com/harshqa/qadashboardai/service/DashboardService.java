package com.harshqa.qadashboardai.service;

import com.harshqa.qadashboardai.dto.FailureStatDto;
import com.harshqa.qadashboardai.dto.TrendDto;
import com.harshqa.qadashboardai.entity.TestFailure;
import com.harshqa.qadashboardai.entity.TestRun;
import com.harshqa.qadashboardai.repository.TestCaseRepository;
import com.harshqa.qadashboardai.repository.TestRunRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TestRunRepository testRunRepository;
    private final TestCaseRepository testCaseRepository;

    public DashboardService(TestRunRepository testRunRepository, TestCaseRepository testCaseRepository) {
        this.testRunRepository = testRunRepository;
        this.testCaseRepository = testCaseRepository;
    }

    public List<TrendDto> getTrendAnalysis(int days) {
        // 1. Calculate the cutoff date
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);

        // 2. Fetch runs from DB
        List<TestRun> runs = testRunRepository.findAllByExecutionDateAfter(cutoff);

        // 3. Transform to DTOs and Sort by Date
        return runs.stream()
                .sorted(Comparator.comparing(TestRun::getExecutionDate)) // Oldest first
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private TrendDto convertToDto(TestRun run) {
        double passRate = 0.0;
        if (run.getTotalTests() > 0) {
            passRate = (double) run.getPassCount() / run.getTotalTests() * 100;
        }

        return TrendDto.builder()
                .date(run.getExecutionDate().format(DateTimeFormatter.ISO_LOCAL_DATE)) // "2025-12-22"
                .totalTests(run.getTotalTests())
                .passCount(run.getPassCount())
                .failCount(run.getFailCount()) // Includes Skips if you want, or keep separate
                .passRate(Math.round(passRate * 100.0) / 100.0) // Round to 2 decimals
                .build();
    }

    public List<FailureStatDto> getTopFailures(int limit) {
        // Ask repository for the top 'limit' items
        List<Object[]> results = testCaseRepository.findTopFailures(PageRequest.of(0, limit));

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
}