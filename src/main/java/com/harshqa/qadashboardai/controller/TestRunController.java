package com.harshqa.qadashboardai.controller;

import com.harshqa.qadashboardai.entity.TestRun;
import com.harshqa.qadashboardai.model.TestReport;
import com.harshqa.qadashboardai.repository.TestRunRepository;
import com.harshqa.qadashboardai.service.AiAnalysisService;
import com.harshqa.qadashboardai.service.TestRunService;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/runs")
class TestRunController {

    private final TestRunRepository testRunRepository;
    private final AiAnalysisService aiAnalysisService;
    private final TestRunService testRunService;

    public TestRunController(TestRunRepository testRunRepository, AiAnalysisService aiAnalysisService, TestRunService testRunService) {
        this.testRunRepository = testRunRepository;
        this.aiAnalysisService = aiAnalysisService;
        this.testRunService = testRunService;
    }

    // 1. Get All Runs (For the history table) (Supports filters for Dashboard & History Page)
    @GetMapping
    public List<TestRun> getAllRuns(
            @RequestParam(required = false) Integer limit, // e.g. ?limit=5
            @RequestParam(required = false) Integer days   // e.g. ?days=30
    ) {
        // Scenario A: Dashboard Widget -> Get Latest 5
        if (limit != null && limit == 5) {
            return testRunRepository.findTop5ByOrderByExecutionDateDesc();
        }

        // Scenario B: History Page -> Filter by Last X Days
        if (days != null) {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
            return testRunRepository.findAllByExecutionDateAfterOrderByExecutionDateDesc(cutoffDate);
        }

        // Scenario C: Default -> Return All (Sorted Newest First)
        return testRunRepository.findAll(Sort.by(Sort.Direction.DESC, "executionDate"));
    }

    // 2. Get Single Run Details (For the deep dive)
    @GetMapping("/{id}")
    public TestRun getRunById(@PathVariable Long id) {
        return testRunRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Run not found: " + id));
    }

    // Endpoint: POST /api/runs/{id}/analyze
    // Logic: If analysis exists, return it. If not, generate, save, and return.
    @PostMapping("/{id}/analyze-run")
    public String analyzeRun(@PathVariable Long id) {
        TestRun run = testRunRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Run not found: " + id));

        // 1. Check if we already have it (Cache check)
        if (run.getAiAnalysis() != null && !run.getAiAnalysis().isEmpty()) {
            return run.getAiAnalysis();
        }

        // 2. We need to reconstruct the TestReport object for the AI
        // (Since we don't save the full raw XML, we rebuild it from DB entities)
        TestReport reportContext = testRunService.reconstructReportFromDb(run);

        // 3. Generate Analysis
        String analysis = aiAnalysisService.analyze(reportContext)
                .replaceAll("^```(?:json)?\\s*", "") // Remove opening ```
                .replaceAll("\\s*```\\s*$", "");    // Remove closing ```

        // 4. Save to DB (Persistence)
        run.setAiAnalysis(analysis);
        testRunRepository.save(run);

        return analysis;
    }

}
