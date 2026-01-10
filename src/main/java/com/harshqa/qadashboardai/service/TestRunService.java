package com.harshqa.qadashboardai.service;

import com.harshqa.qadashboardai.entity.Project;
import com.harshqa.qadashboardai.entity.TestCase;
import com.harshqa.qadashboardai.entity.TestFailure;
import com.harshqa.qadashboardai.entity.TestRun;
import com.harshqa.qadashboardai.model.FailureDefinition;
import com.harshqa.qadashboardai.model.TestReport;
import com.harshqa.qadashboardai.model.TestCaseDetail;
import com.harshqa.qadashboardai.repository.ProjectRepository;
import com.harshqa.qadashboardai.repository.TestFailureRepository;
import com.harshqa.qadashboardai.repository.TestRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TestRunService {

    private final TestRunRepository testRunRepository;
    private final TestFailureRepository testFailureRepository;
    private final ProjectRepository projectRepository;

    public TestRunService(TestRunRepository testRunRepository, TestFailureRepository testFailureRepository, ProjectRepository projectRepository) {
        this.testRunRepository = testRunRepository;
        this.testFailureRepository = testFailureRepository;
        this.projectRepository = projectRepository;
    }

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
    }

    /**
     * Smart Save:
     * 1. Checks if a run exists for the same DATE (ignoring timestamp).
     * 2. If exists -> MERGE (Update only failed tests based on new XML).
     * 3. If new -> CREATE (Standard save).
     */
    @Transactional // Ensures either everything saves or nothing saves (Atomic)
    public Long saveTestRun(TestReport report, Long projectId) {

        // 1. Fetch Project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        LocalDateTime reportDate = report.getTimestamp();
        if (reportDate == null) reportDate = LocalDateTime.now();

        // Calculate Start and End of the Day to find existing run
        LocalDateTime startOfDay = reportDate.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = reportDate.toLocalDate().atTime(LocalTime.MAX);

        // Fetch runs for this date
        List<TestRun> existingRuns = testRunRepository.findAllByExecutionDateBetweenAndProject(startOfDay, endOfDay, project);

        if (!existingRuns.isEmpty()) {
            // MERGE STRATEGY: Update the existing run (Taking the first match if multiple exist)
            TestRun existingRun = existingRuns.getFirst();
            return mergeWithExistingRun(existingRun, report);
        } else {
            // CREATE STRATEGY: Standard new entry
            return createNewTestRun(report, project);
        }

    }

    private Long createNewTestRun(TestReport report, Project project) {
        // Map POJO -> Entity
        TestRun run = new TestRun();
        run.setProject(project); // Set the project

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

    private Long mergeWithExistingRun(TestRun existingRun, TestReport rerunReport) {
        // 1. Filter only currently FAILED tests in the DB
        List<TestCase> existingFailures = existingRun.getTestCases().stream()
                .filter(tc -> "FAILED".equalsIgnoreCase(tc.getStatus()))
                .collect(Collectors.toList());

        if (existingFailures.isEmpty()) {
            return existingRun.getId(); // Nothing to fix
        }

        // 2. Create a Map of the Rerun Report for fast lookup
        // Key: ClassName + "#" + TestName
        Map<String, TestCaseDetail> rerunMap = rerunReport.getPassedTests().stream()
                .collect(Collectors.toMap(
                        tc -> tc.getClassName() + "#" + tc.getTestName(),
                        tc -> tc,
                        (existing, replacement) -> existing // Keep first if duplicates occur
                ));
        rerunMap.putAll(rerunReport.getFailedTests().stream()
                .collect(Collectors.toMap(
                        tc -> tc.getClassName() + "#" + tc.getTestName(),
                        tc -> tc,
                        (existing, replacement) -> existing // Keep first if duplicates occur
                )));

        int fixedCount = 0;

        // 3. Iterate through DB failures and check against Rerun XML
        for (TestCase dbFailure : existingFailures) {
            String key = dbFailure.getClassName() + "#" + dbFailure.getTestName();
            int kanIndex = key.indexOf("_KAN");
            if (kanIndex != -1) {
                key = key.substring(0, kanIndex);
            }
            boolean shouldUpdateToPassed = false;

            if (!rerunMap.containsKey(key)) {
                // CASE i: Test NOT in Rerun XML -> Assume it passed in an intermediate run
                shouldUpdateToPassed = true;
            } else {
                // CASE ii: Test IS in Rerun XML -> Check if it passed this time
                TestCaseDetail rerunDetail = rerunMap.get(key);
                if (rerunDetail.getFailureRefId() == null || "PASSED".equalsIgnoreCase(rerunDetail.getStatus())) {
                    shouldUpdateToPassed = true;
                }
            }

            // 4. Apply Update
            if (shouldUpdateToPassed) {
                dbFailure.setStatus("PASSED");
                dbFailure.setTestFailure(null); // Clear the failure details
                fixedCount++;
            }
        }

        // 5. Update Run Totals if any tests were fixed
        if (fixedCount > 0) {
            existingRun.setPassCount(existingRun.getPassCount() + fixedCount);
            existingRun.setFailCount(Math.max(0, existingRun.getFailCount() - fixedCount));
            testRunRepository.save(existingRun);
            System.out.println("Merged Rerun: Fixed " + fixedCount + " failures for Run ID " + existingRun.getId());
        }

        return existingRun.getId();
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

    // Delete by ID
    @Transactional
    public void deleteRunById(Long id) {
        if (testRunRepository.existsById(id)) {
            testRunRepository.deleteById(id);
        } else {
            throw new RuntimeException("Test Run not found with ID: " + id);
        }
    }

    // NEW: Delete by Date (YYYY-MM-DD)
    @Transactional
    public void deleteRunByDate(LocalDate date, Long projectId) {
        Project project = getProject(projectId);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<TestRun> runs = testRunRepository.findAllByProjectAndExecutionDateBetween(project, startOfDay, endOfDay);
        if (runs.isEmpty()) {
            throw new RuntimeException("No Test Run found for date: " + date);
        }

        testRunRepository.deleteAll(runs);
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