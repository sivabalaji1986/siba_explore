package com.ops.chat.service;

import com.ops.chat.model.ChatSession;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.StreamingResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Drives the LangChain4j streaming call and pipes each token to the SSE emitter.
 *
 * Flow per request:
 *  1. Build full message history (system prompt + past turns + current user message)
 *  2. Call the LLM with a StreamingResponseHandler
 *  3. onNext  → send token as SSE event
 *  4. onComplete → persist AI reply to session history, send [DONE] sentinel
 *  5. onError → send error event, complete emitter exceptionally
 */
@Service
public class ChatStreamingService {

    private static final Logger log = LoggerFactory.getLogger(ChatStreamingService.class);

    private static final String DONE_SENTINEL = "[DONE]";

    private final StreamingChatLanguageModel llm;
    private final SessionManager sessionManager;
    private final PromptBuilder promptBuilder;

    public ChatStreamingService(
        StreamingChatLanguageModel llm,
        SessionManager sessionManager,
        PromptBuilder promptBuilder
    ) {
        this.llm            = llm;
        this.sessionManager = sessionManager;
        this.promptBuilder  = promptBuilder;
    }

    /**
     * Streams LLM tokens into the provided SseEmitter.
     *
     * @param sessionId  the session to use for history lookup
     * @param userMessage the agent's latest message
     * @param intent      intent string (used if no session found — fallback)
     * @param emitter     the SSE emitter to write tokens to
     */
    public void streamToEmitter(
        String sessionId,
        String userMessage,
        String intent,
        SseEmitter emitter
    ) {
        // 1. Resolve session
        ChatSession session = sessionManager.getOrThrow(sessionId);

        // 2. Persist user message to history
        sessionManager.addUserMessage(sessionId, userMessage);

        // 3. Build full prompt (system + history including current user message)
        String systemPrompt = promptBuilder.buildSystemPrompt(
            session.getIntent(), session.getAgentId(), session.getCustomerId()
        );
        List<ChatMessage> messages = session.getHistoryWithSystem(systemPrompt);

        log.debug("LLM call: session={} turns={} intent={}",
            sessionId, session.getHistorySize(), session.getIntent());

        // 4. Buffer to collect the full response for history persistence
        StringBuilder fullResponse = new StringBuilder();

        // 5. Call the LLM — non-blocking, tokens arrive via the handler
        CompletableFuture<Void> future = new CompletableFuture<>();

        llm.generate(messages, new StreamingResponseHandler<>() {

            @Override
            public void onNext(String token) {
                fullResponse.append(token);
                try {
                    // Send each token as a plain SSE data event
                    emitter.send(
                        SseEmitter.event()
                            .name("token")
                            .data(token)
                    );
                } catch (IOException e) {
                    log.warn("Client disconnected mid-stream: session={}", sessionId);
                    future.completeExceptionally(e);
                }
            }

            @Override
            public void onComplete(Response<dev.langchain4j.data.message.AiMessage> response) {
                // Persist the complete AI reply to session history
                sessionManager.addAssistantMessage(sessionId, fullResponse.toString());

                try {
                    // Send [DONE] sentinel so the client knows the stream is finished
                    emitter.send(
                        SseEmitter.event()
                            .name("token")
                            .data(DONE_SENTINEL)
                    );
                    emitter.complete();
                    log.debug("Stream complete: session={} tokens≈{}",
                        sessionId, fullResponse.length());
                } catch (IOException e) {
                    log.warn("Error sending DONE sentinel: {}", e.getMessage());
                } finally {
                    future.complete(null);
                }
            }

            @Override
            public void onError(Throwable error) {
                log.error("LLM error for session {}: {}", sessionId, error.getMessage(), error);
                try {
                    emitter.send(
                        SseEmitter.event()
                            .name("error")
                            .data("LLM error: " + error.getMessage())
                    );
                    emitter.completeWithError(error);
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                } finally {
                    future.completeExceptionally(error);
                }
            }
        });
    }
}
