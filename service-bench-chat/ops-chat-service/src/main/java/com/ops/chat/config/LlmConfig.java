package com.ops.chat.config;

import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires up the LangChain4j StreamingChatLanguageModel bean.
 *
 * Set llm.provider=claude  → uses Anthropic Claude
 * Set llm.provider=openai  → uses OpenAI GPT
 */
@Configuration
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    @Value("${llm.provider:claude}")
    private String provider;

    // ── Claude ────────────────────────────────────────────────────────────────
    @Value("${anthropic.api-key:}")
    private String anthropicApiKey;

    @Value("${anthropic.model:claude-3-5-sonnet-20241022}")
    private String anthropicModel;

    @Value("${anthropic.max-tokens:2048}")
    private int anthropicMaxTokens;

    @Value("${anthropic.temperature:0.7}")
    private double anthropicTemperature;

    // ── OpenAI ────────────────────────────────────────────────────────────────
    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Value("${openai.model:gpt-4o}")
    private String openAiModel;

    @Value("${openai.max-tokens:2048}")
    private int openAiMaxTokens;

    @Value("${openai.temperature:0.7}")
    private double openAiTemperature;

    @Bean
    public StreamingChatLanguageModel streamingChatModel() {
        return switch (provider.toLowerCase()) {
            case "openai" -> buildOpenAiModel();
            default       -> buildClaudeModel();
        };
    }

    private StreamingChatLanguageModel buildClaudeModel() {
        log.info("Configuring LLM: Anthropic Claude ({})", anthropicModel);
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            log.warn("ANTHROPIC_API_KEY is not set — LLM calls will fail at runtime");
        }
        return AnthropicStreamingChatModel.builder()
            .apiKey(anthropicApiKey)
            .modelName(anthropicModel)
            .maxTokens(anthropicMaxTokens)
            .temperature(anthropicTemperature)
            .build();
    }

    private StreamingChatLanguageModel buildOpenAiModel() {
        log.info("Configuring LLM: OpenAI ({})", openAiModel);
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            log.warn("OPENAI_API_KEY is not set — LLM calls will fail at runtime");
        }
        return OpenAiStreamingChatModel.builder()
            .apiKey(openAiApiKey)
            .modelName(openAiModel)
            .maxTokens(openAiMaxTokens)
            .temperature(openAiTemperature)
            .build();
    }
}
