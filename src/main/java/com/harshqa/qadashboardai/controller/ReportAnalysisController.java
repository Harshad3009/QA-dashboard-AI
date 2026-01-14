package com.harshqa.qadashboardai.controller;

import com.harshqa.qadashboardai.model.FailureDefinition;
import com.harshqa.qadashboardai.model.TestReport;
import com.harshqa.qadashboardai.service.AiAnalysisService;
import com.harshqa.qadashboardai.service.TestRunService;
import com.harshqa.qadashboardai.service.XmlParserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ReportAnalysisController {

    private final XmlParserService xmlParserService;
    private final AiAnalysisService aiAnalysisService;
    private final TestRunService testRunService;

    // Dependency Injection: We ask Spring for the parser we just made
    public ReportAnalysisController(XmlParserService xmlParserService, AiAnalysisService aiAnalysisService, TestRunService testRunService) {
        this.xmlParserService = xmlParserService;
        this.aiAnalysisService = aiAnalysisService;
        this.testRunService = testRunService;
    }

    @PostMapping("/upload-report")
    public List<Long> uploadReport(@RequestParam("files") MultipartFile[] files,
                                   @RequestParam(value = "projectId", required = false) Long projectId) {
        // --- Determine Project ID ---
        Long finalProjectId = projectId;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // If authenticated via API Key, the "credentials" field holds the Project ID (set in Filter)
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SYSTEM"))) {
            finalProjectId = (Long) auth.getCredentials();
        }

        if (finalProjectId == null) {
            throw new RuntimeException("Project ID is required (either via parameter or API Key)");
        }
        // ----------------------------
        List<Long> runIds = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                // Parse the XML to Java Object
                TestReport report = xmlParserService.parse(file.getInputStream());

                // SAVE to Database
                Long runId = testRunService.saveTestRun(report, finalProjectId);
                System.out.println("Report saved for Project " + finalProjectId + " with ID: " + runId);

                // Add the IDs to array to return after processing all the files.
                runIds.add(runId);
            }
            return runIds;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to process report: " + e.getMessage());
        }
    }

    @PostMapping("/test-ai-analysis")
    public String testAiAnalysisWithDummyReport() {
        // 1. Create a dummy tiny report
        TestReport dummyReport = new TestReport();
        dummyReport.setTotalTests(5);
        dummyReport.setFailCount(1);
        dummyReport.setPassCount(4);
        dummyReport.setTotalDuration(10.5);

        FailureDefinition fakeFailure = FailureDefinition.builder()
                .id("ERR_TEST")
                .message("Test Error")
                .stackTrace("java.lang.NullPointer at Com.Test.Main")
                .occurrenceCount(1)
                .build();

        dummyReport.getFailureCatalog().add(fakeFailure);

        // 2. Try to analyze it
        try {
            return aiAnalysisService.analyze(dummyReport);
        } catch (Exception e) {
            // Log the FULL error to console
            e.printStackTrace();
            return "Failed: " + e.getMessage();
        }
    }
}
