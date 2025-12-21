package com.harshqa.qadashboardai.controller;

import com.harshqa.qadashboardai.model.TestReport;
import com.harshqa.qadashboardai.service.XmlParserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ReportAnalysisController {

    private final XmlParserService xmlParserService;

    // Dependency Injection: We ask Spring for the parser we just made
    public ReportAnalysisController(XmlParserService xmlParserService) {
        this.xmlParserService = xmlParserService;
    }

    @PostMapping("/analyze")
    public TestReport analyzeReport(@RequestParam("file") MultipartFile file) {
        try {
            // 1. Get the input stream from the uploaded file
            // 2. Pass it to our service
            return xmlParserService.parse(file.getInputStream());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML: " + e.getMessage());
        }
    }
}
