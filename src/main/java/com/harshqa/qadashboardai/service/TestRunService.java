package com.harshqa.qadashboardai.service;

import com.harshqa.qadashboardai.entity.TestCase;
import com.harshqa.qadashboardai.entity.TestRun;
import com.harshqa.qadashboardai.model.TestReport;
import com.harshqa.qadashboardai.model.TestCaseDetail;
import com.harshqa.qadashboardai.repository.TestRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TestRunService {

    private final TestRunRepository testRunRepository;

    public TestRunService(TestRunRepository testRunRepository) {
        this.testRunRepository = testRunRepository;
    }

    @Transactional // Ensures either everything saves or nothing saves (Atomic)
    public Long saveTestRun(TestReport report) {
        // 1. Map POJO -> Entity
        TestRun run = new TestRun();
        run.setExecutionDate(report.getTimestamp() != null ? report.getTimestamp() : LocalDateTime.now());
        run.setTotalTests(report.getTotalTests());
        run.setPassCount(report.getPassCount());
        run.setFailCount(report.getFailCount());
        run.setSkipCount(report.getSkipCount());
        run.setTotalDuration(report.getTotalDuration());

        // 2. Map Test Cases
        // Combine all lists (Pass, Fail, Skip) into one DB list
        mapTestCases(run, report.getPassedTests(), "PASS");
        mapTestCases(run, report.getFailedTests(), "FAIL");
        mapTestCases(run, report.getSkippedTests(), "SKIP");

        // 3. Save to DB (Cascade will save all test cases too)
        TestRun savedRun = testRunRepository.save(run);

        System.out.println("Saved Run ID: " + savedRun.getId());
        return savedRun.getId();
    }

    private void mapTestCases(TestRun run, java.util.List<TestCaseDetail> details, String status) {
        if (details == null) return;

        for (TestCaseDetail detail : details) {
            TestCase testCase = new TestCase();
            testCase.setTestName(detail.getTestName());
            testCase.setClassName(detail.getClassName());
            testCase.setDuration(detail.getDuration());
            testCase.setStatus(status);

            // If failed, we need to find the failure message from the catalog
            // For now, we'll leave it simple, or you can pass the map here.
            // testCase.setFailureMessage(...);

            testCase.setTestRun(run); // Link Child -> Parent
            run.getTestCases().add(testCase); // Link Parent -> Child
        }
    }
}