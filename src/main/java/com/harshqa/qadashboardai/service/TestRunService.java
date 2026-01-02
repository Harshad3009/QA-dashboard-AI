package com.harshqa.qadashboardai.service;

import com.harshqa.qadashboardai.entity.TestCase;
import com.harshqa.qadashboardai.entity.TestFailure;
import com.harshqa.qadashboardai.entity.TestRun;
import com.harshqa.qadashboardai.model.FailureDefinition;
import com.harshqa.qadashboardai.model.TestReport;
import com.harshqa.qadashboardai.model.TestCaseDetail;
import com.harshqa.qadashboardai.repository.TestFailureRepository;
import com.harshqa.qadashboardai.repository.TestRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TestRunService {

    private final TestRunRepository testRunRepository;
    private final TestFailureRepository testFailureRepository;

    public TestRunService(TestRunRepository testRunRepository, TestFailureRepository testFailureRepository) {
        this.testRunRepository = testRunRepository;
        this.testFailureRepository = testFailureRepository;
    }

    @Transactional // Ensures either everything saves or nothing saves (Atomic)
    public Long saveTestRun(TestReport report) {
        // Map POJO -> Entity
        TestRun run = new TestRun();
        run.setExecutionDate(report.getTimestamp() != null ? report.getTimestamp() : LocalDateTime.now());
        run.setTotalTests(report.getTotalTests());
        run.setPassCount(report.getPassCount());
        run.setFailCount(report.getFailCount());
        run.setSkipCount(report.getSkipCount());
        run.setTotalDuration(report.getTotalDuration());

        // Create a Lookup Map (ID -> Stack Trace)
        // This turns the list [FailureDefinition(id="ERR_1", stackTrace="...")]
        // into a Map {"ERR_1": "..."} for fast access.
        Map<String, String> failureMap = report.getFailureCatalog().stream()
                .collect(Collectors.toMap(FailureDefinition::getId, FailureDefinition::getStackTrace));

        // Map Test Cases
        // Combine all lists (Pass, Fail, Skip) into one DB list
        mapTestCases(run, report.getPassedTests(), "PASSED", Collections.emptyMap());
        mapTestCases(run, report.getFailedTests(), "FAILED", failureMap);
        mapTestCases(run, report.getSkippedTests(), "SKIPPED", Collections.emptyMap());

        // Save to DB (Cascade will save all test cases too)
        TestRun savedRun = testRunRepository.save(run);
        System.out.println("Saved Run ID: " + savedRun.getId());
        return savedRun.getId();
    }

    private void mapTestCases(TestRun run, List<TestCaseDetail> details, String status, Map<String, String> failureMap) {
        if (details == null) return;

        for (TestCaseDetail detail : details) {
            TestCase testCase = new TestCase();
            testCase.setTestName(detail.getTestName());
            testCase.setClassName(detail.getClassName());
            testCase.setDuration(detail.getDuration());
            testCase.setStatus(status);

            // If this test failed, look up the stack trace using the Ref ID
            if ("FAILED".equals(status) && detail.getFailureRefId() != null) {
                String stackTrace = failureMap.get(detail.getFailureRefId());
                if (stackTrace != null) {
                    TestFailure failureEntity = getOrCreateFailure(stackTrace, detail.getFailureRefId()); // You might pass just stackTrace
                    testCase.setTestFailure(failureEntity);
                }
            }

            testCase.setTestRun(run); // Link Child -> Parent
            run.getTestCases().add(testCase); // Link Parent -> Child
        }
    }

    /**
     * Checks if this error exists in DB. If yes, returns it. If no, creates new.
     */
    private TestFailure getOrCreateFailure(String stackTrace, String messageHelper) {
        // 1. Generate Hash
        String hash = generateHash(stackTrace);

        // 2. Check DB Cache
        Optional<TestFailure> existing = testFailureRepository.findByFailureHash(hash);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 3. Create New
        TestFailure failure = new TestFailure();
        failure.setFailureHash(hash);
        failure.setStackTrace(stackTrace);
        // We use the first line or a substring as the short message
        failure.setMessage(stackTrace.length() > 100 ? stackTrace.substring(0, 100) : stackTrace);

        return testFailureRepository.save(failure);
    }

    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    public TestReport reconstructReportFromDb(TestRun run) {
        TestReport report = new TestReport();
        report.setTotalTests(run.getTotalTests());
        report.setPassCount(run.getPassCount());
        report.setFailCount(run.getFailCount());
        report.setSkipCount(run.getSkipCount());
        report.setTotalDuration(run.getTotalDuration());
        report.setTimestamp(run.getExecutionDate());

        // Reconstruct the Failure Catalog
        // We only need failed tests for analysis
        List<TestCaseDetail> failedTests = run.getTestCases().stream()
                .filter(tc -> "FAILED".equals(tc.getStatus()))
                .map(tc -> {
                    // Rebuild the detail object
                    return TestCaseDetail.builder()
                            .testName(tc.getTestName())
                            .className(tc.getClassName())
                            .duration(tc.getDuration())
                            .status("FAILED")
                            // If we have a linked failure, use its hash/ID as reference
                            .failureRefId(tc.getTestFailure() != null ? tc.getTestFailure().getFailureHash() : null)
                            .build();
                })
                .collect(Collectors.toList());

        // Also rebuild the distinct failure catalog from the DB entities
        List<FailureDefinition> catalog = run.getTestCases().stream()
                .filter(tc -> "FAILED".equals(tc.getStatus()) && tc.getTestFailure() != null)
                .map(tc -> tc.getTestFailure())
                .distinct()
                .map(f -> FailureDefinition.builder()
                        .id(f.getFailureHash())
                        .message(f.getMessage())
                        .stackTrace(f.getStackTrace())
                        .build())
                .collect(Collectors.toList());

        report.setFailedTests(failedTests);
        report.setFailureCatalog(catalog);

        return report;
    }
}