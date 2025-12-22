package com.harshqa.qadashboardai.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestReport {

    // Summary Stats
    private int totalTests;
    private int passCount;
    private int failCount;
    private int skipCount;
    private double totalDuration;

    // The Unique Dictionary of Errors
    private List<FailureDefinition> failureCatalog = new ArrayList<>();

    // The Containers
    private List<TestCaseDetail> passedTests = new ArrayList<>();
    private List<TestCaseDetail> failedTests = new ArrayList<>();
    private List<TestCaseDetail> skippedTests = new ArrayList<>();

}
