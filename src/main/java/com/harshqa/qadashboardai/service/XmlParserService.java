package com.harshqa.qadashboardai.service;

import com.harshqa.qadashboardai.model.TestReport;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

@Service
public class XmlParserService {

    public TestReport parse(InputStream xmlInputStream) throws Exception {
        TestReport report = new TestReport();

        // 1. Create the XML Parser
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // 2. Parse the input stream into a "Document" tree
        Document doc = builder.parse(xmlInputStream);
        doc.getDocumentElement().normalize();

        // 3. Extract High-Level Stats (usually found in <testsuite> tag)
        // Note: Some XMLs have multiple suites; this example grabs the first main one
        Element suiteElement = (Element) doc.getElementsByTagName("testsuite").item(0);

        if (suiteElement != null) {
            report.setTotalTests(Integer.parseInt(suiteElement.getAttribute("tests")));
            report.setFailures(Integer.parseInt(suiteElement.getAttribute("failures")));
            report.setErrors(Integer.parseInt(suiteElement.getAttribute("errors")));
            report.setSkipped(Integer.parseInt(suiteElement.getAttribute("skipped")));
            report.setTime(Double.parseDouble(suiteElement.getAttribute("time")));
        }

        // 4. Extract Failure Messages
        // We look for every <failure> tag inside the document
        NodeList failureNodes = doc.getElementsByTagName("failure");

        for (int i = 0; i < failureNodes.getLength(); i++) {
            Element failureElement = (Element) failureNodes.item(i);
            // Get the text content (the stack trace) and the message attribute
            String message = failureElement.getAttribute("message");
            String stackTrace = failureElement.getTextContent();

            // Add to our list (Truncating stack trace to save AI tokens if needed)
            report.getFailureMessages().add("Error: " + message + "\nTrace: " + stackTrace.trim());
        }

        return report;
    }

}
