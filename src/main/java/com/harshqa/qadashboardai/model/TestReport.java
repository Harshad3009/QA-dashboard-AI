package com.harshqa.qadashboardai.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestReport {

    private int totalTests;
    private int failures;
    private int errors;
    private int skipped;
    private double time;
    private List<String> failureMessages = new ArrayList<>();

}
