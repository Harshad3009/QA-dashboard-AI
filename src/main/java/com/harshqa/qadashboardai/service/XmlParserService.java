package com.harshqa.qadashboardai.service;

import com.harshqa.qadashboardai.model.FailureDefinition;
import com.harshqa.qadashboardai.model.TestCaseDetail;
import com.harshqa.qadashboardai.model.TestReport;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class XmlParserService {

    public TestReport parse(InputStream xmlInputStream) throws Exception {
        TestReport report = new TestReport();

        // Maps unique stack trace content -> to our generated ID (e.g., "ERR_1")
        // We use this to check if we've seen an error before.
        Map<String, String> stackTraceToIdMap = new HashMap<>();

        // Helper map to quickly find the Definition object to increment count
        Map<String, FailureDefinition> idToDefinitionMap = new HashMap<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlInputStream);
        doc.getDocumentElement().normalize();

        // 1. Process Metadata (High Level Stats)
        Element suiteElement = (Element) doc.getElementsByTagName("testsuite").item(0);
        if (suiteElement != null) {
            report.setTotalDuration(Double.parseDouble(getAttribute(suiteElement, "time", "0.0")));
        }

        // 2. Iterate ALL test cases
        NodeList testCases = doc.getElementsByTagName("testcase");
        int errorCounter = 1; // Used to generate IDs like ERR_1, ERR_2

        for (int i = 0; i < testCases.getLength(); i++) {
            Element testElement = (Element) testCases.item(i);

            String name = testElement.getAttribute("name");
            String className = testElement.getAttribute("classname");
            double duration = Double.parseDouble(getAttribute(testElement, "time", "0.0"));

            // Check for children to see status
            NodeList failureNodes = testElement.getElementsByTagName("failure");
            NodeList errorNodes = testElement.getElementsByTagName("error"); // Treat errors as failures
            NodeList skippedNodes = testElement.getElementsByTagName("skipped");

            boolean isFailure = failureNodes.getLength() > 0;
            boolean isError = errorNodes.getLength() > 0;
            boolean isSkipped = skippedNodes.getLength() > 0;

            if (isFailure || isError) {
                // Determine which node has the details (failure or error)
                Element failNode = isFailure ? (Element) failureNodes.item(0) : (Element) errorNodes.item(0);

                String rawMessage = failNode.getAttribute("message");
                String rawTrace = failNode.getTextContent().trim();

                // --- SAFETY TRUNCATION ---
                // If trace is huge, cut it off. 1000 chars is usually enough for the Root Cause.
                if (rawTrace.length() > 1000) {
                    rawTrace = rawTrace.substring(0, 1000) + "\n... [Truncated by QA Dashboard]";
                }

                // --- DEDUPLICATION LOGIC ---
                // We use the stack trace as the "Key" to identify uniqueness.
                String uniqueKey = rawTrace;
                if (uniqueKey.isEmpty()) uniqueKey = rawMessage; // Fallback if trace is empty

                String failureId;

                if (stackTraceToIdMap.containsKey(uniqueKey)) {
                    // Case A: We have seen this error before!
                    failureId = stackTraceToIdMap.get(uniqueKey);

                    // Increment the counter for this existing error
                    FailureDefinition def = idToDefinitionMap.get(failureId);
                    def.setOccurrenceCount(def.getOccurrenceCount() + 1);
                } else {
                    // Case B: This is a NEW unique error!
                    failureId = "ERR_" + errorCounter++;
                    stackTraceToIdMap.put(uniqueKey, failureId);

                    FailureDefinition newDef = FailureDefinition.builder()
                            .id(failureId)
                            .message(rawMessage)
                            .stackTrace(rawTrace) // We capture the heavy text ONCE
                            .occurrenceCount(1)
                            .build();

                    report.getFailureCatalog().add(newDef);
                    idToDefinitionMap.put(failureId, newDef);
                }

                // Add to Failed List
                report.getFailedTests().add(TestCaseDetail.builder()
                        .testName(name)
                        .className(className)
                        .duration(duration)
                        .status("FAIL")
                        .failureRefId(failureId) // LINKING HAPPENS HERE
                        .build());

            } else if (isSkipped) {
                // Add to Passed list (or separate Skipped list if you prefer)
                // For now, adding to Passed but marking status SKIP per requirements logic
                report.getSkippedTests().add(TestCaseDetail.builder()
                        .testName(name)
                        .className(className)
                        .duration(duration)
                        .status("SKIP")
                        .build());
            } else {
                // Happy Path
                report.getPassedTests().add(TestCaseDetail.builder()
                        .testName(name)
                        .className(className)
                        .duration(duration)
                        .status("PASS")
                        .build());
            }
        }

        // Final Calculations (Truth based on parsing)
        report.setPassCount(report.getPassedTests().size());
        report.setFailCount(report.getFailedTests().size()); // Includes both Errors & Failures
        report.setSkipCount(report.getSkippedTests().size());
        report.setTotalTests(report.getPassCount() + report.getFailCount() + report.getSkipCount());


        return report;
    }

    // Helper to safely get attributes without null pointer exceptions
    private String getAttribute(Element e, String attr, String defaultVal) {
        String val = e.getAttribute(attr);
        return (val == null || val.isEmpty()) ? defaultVal : val;
    }

}
