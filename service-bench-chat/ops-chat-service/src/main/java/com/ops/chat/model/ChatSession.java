package com.ops.chat.model;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents one conversation session between a customer care agent and the LLM.
 * Holds the full message history so the LLM maintains context across turns.
 */
public class ChatSession {

    public enum Intent {
        ADDRESS_UPDATE,
        ACCOUNT_ACTIVATION,
        GENERAL
    }

    private final String sessionId;
    private final String agentId;
    private final String customerId;
    private final Intent intent;
    private final Instant createdAt;
    private volatile Instant lastActivityAt;

    // Full message history passed to the LLM on every turn
    private final List<ChatMessage> history = new ArrayList<>();

    public ChatSession(String sessionId, String agentId, String customerId, Intent intent) {
        this.sessionId      = sessionId;
        this.agentId        = agentId;
        this.customerId     = customerId;
        this.intent         = intent;
        this.createdAt      = Instant.now();
        this.lastActivityAt = Instant.now();
    }

    // ── History management ────────────────────────────────────────────────────

    public synchronized void addUserMessage(String text) {
        history.add(UserMessage.from(text));
        lastActivityAt = Instant.now();
    }

    public synchronized void addAssistantMessage(String text) {
        history.add(AiMessage.from(text));
        lastActivityAt = Instant.now();
    }

    /**
     * Returns the full message list to be passed to the LLM:
     * [SystemMessage] + [turn 1 user] + [turn 1 ai] + ... + [current user]
     */
    public synchronized List<ChatMessage> getHistoryWithSystem(String systemPrompt) {
        List<ChatMessage> full = new ArrayList<>();
        full.add(SystemMessage.from(systemPrompt));
        full.addAll(history);
        return Collections.unmodifiableList(full);
    }

    public synchronized List<ChatMessage> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    public synchronized void trimHistory(int maxMessages) {
        // Keep most recent messages, but always keep the first user message for context
        while (history.size() > maxMessages) {
            history.remove(0);
        }
    }

    public synchronized int getHistorySize() {
        return history.size();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getSessionId()      { return sessionId; }
    public String getAgentId()        { return agentId; }
    public String getCustomerId()     { return customerId; }
    public Intent getIntent()         { return intent; }
    public Instant getCreatedAt()     { return createdAt; }
    public Instant getLastActivityAt(){ return lastActivityAt; }

    @Override
    public String toString() {
        return String.format("ChatSession{id=%s, agent=%s, customer=%s, intent=%s, turns=%d}",
            sessionId, agentId, customerId, intent, history.size() / 2);
    }
}
