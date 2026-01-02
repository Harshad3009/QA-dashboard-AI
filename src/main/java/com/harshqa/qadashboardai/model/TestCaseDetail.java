package com.harshqa.qadashboardai.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestCaseDetail {

    private String testName;
    private String className;
    private double duration;
    private String status;          // "PASSED", "FAILED", "SKIPPED"
    private String failureRefId;    // Points to FailureDefinition (e.g., "ERR_1"). Null if passed.

}
