package com.harshqa.qadashboardai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QaDashboardAiApplication {

    private final ChatLanguageModel chatModel;

    // Spring automagically injects the Gemini implementation here!
    public QaDashboardAiApplication(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/ask-ai")
    public String askAi(@RequestParam(defaultValue = "Explain the difference between verify and assert in testing") String prompt) {
        // This now talks to Google Gemini
        return chatModel.generate(prompt);
    }
}
