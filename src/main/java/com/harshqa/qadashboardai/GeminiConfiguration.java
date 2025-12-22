package com.harshqa.qadashboardai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class GeminiConfiguration {

    @Value("${langchain4j.google-ai-gemini.chat-model.api-key}")
    private String geminiApiKey;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName("gemini-2.5-flash-lite")
                .timeout(Duration.ofMinutes(2))
                .logRequestsAndResponses(true)
                .build();
    }

}
