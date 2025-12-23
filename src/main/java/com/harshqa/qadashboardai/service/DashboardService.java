package com.harshqa.qadashboardai.service;

import com.harshqa.qadashboardai.dto.TrendDto;
import com.harshqa.qadashboardai.entity.TestRun;
import com.harshqa.qadashboardai.repository.TestRunRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TestRunRepository testRunRepository;

    public DashboardService(TestRunRepository testRunRepository) {
        this.testRunRepository = testRunRepository;
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
}