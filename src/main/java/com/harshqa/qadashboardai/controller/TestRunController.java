package com.harshqa.qadashboardai.controller;

import com.harshqa.qadashboardai.entity.TestRun;
import com.harshqa.qadashboardai.repository.TestRunRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RestController
@RequestMapping("/api/runs")
class TestRunController {

    private final TestRunRepository testRunRepository;

    public TestRunController(TestRunRepository testRunRepository) {
        this.testRunRepository = testRunRepository;
    }

    // 1. Get All Runs (For the history table)
    @GetMapping
    public List<TestRun> getAllRuns() {
        // This fetches EVERYTHING from the test_runs table
        return testRunRepository.findAll();
    }

    // 2. Get Single Run Details (For the deep dive)
    @GetMapping("/{id}")
    public TestRun getRunById(@PathVariable Long id) {
        return testRunRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test Run not found with ID: " + id));
    }

}
