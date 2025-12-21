package com.harshqa.qadashboardai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloAiController {

    private final ChatLanguageModel chatModel;

    // Spring automagically injects the Gemini implementation here!
    public HelloAiController(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/")
    public String home() {
        return "QA Dashboard AI is running! Go to /ask-ai to chat.";
    }

    @GetMapping("/ask-ai")
    public String askAi(@RequestParam(defaultValue = "Explain the difference between verify and assert in testing") String prompt) {
        // This now talks to Google Gemini
        return chatModel.generate(prompt);
    }
}
