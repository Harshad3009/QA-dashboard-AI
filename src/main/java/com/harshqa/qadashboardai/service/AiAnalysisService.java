package com.harshqa.qadashboardai.service;

import com.harshqa.qadashboardai.model.TestReport;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class AiAnalysisService {

    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper; // Spring's built-in tool to convert Java Objects -> JSON String

    public AiAnalysisService(ChatLanguageModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    public String analyze(TestReport report) {
        try {
            // 1. Convert our Java Object to a JSON String so the AI can read it
            String jsonContext = objectMapper.writeValueAsString(report);

            // --- DEBUG LOGGING ---
            System.out.println("Payload size: " + jsonContext.length() + " characters.");
            // ---------------------

            // 2. Construct the Prompt (The most important part!)
            String prompt = """
                You are a Senior QA Automation Lead. I am providing you with a test execution report in JSON format.
                
                Your Goal: Analyze the failures and provide a structured summary.
                
                Instructions:
                1. Look at the 'failureCatalog' to understand the root causes.
                2. Group the 'failedTests' by these root causes.
                3. Ignore the 'passedTests' list for the failure analysis (use it only for pass rate context).
                
                Return a JSON response (valid JSON only, no markdown formatting) with the following structure:
                {
                    "executiveSummary": "A 2-sentence high-level summary of the build health.",
                    "failureAnalysis": [
                        {
                            "rootCause": "Brief description of the error (e.g., Timeout in Login)",
                            "count": 5,
                            "affectedFeatures": ["Login", "Dashboard"],
                            "suggestedFix": "What should the developer check?"
                        }
                    ],
                    "flakinessCheck": "Review if any failures look like timing issues or flakiness."
                }
                
                Here is the Report Data:
                %s
                """.formatted(jsonContext);

            // 3. Send to Gemini and return the response
            return chatModel.generate(prompt);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error during AI Analysis: " + e.getMessage());
        }
    }
}