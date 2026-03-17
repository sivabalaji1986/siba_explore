package com.ops.chat.service;

import com.ops.chat.model.ChatSession;
import com.ops.chat.model.ChatSession.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle of chat sessions.
 *
 * - Creates sessions with a unique ID
 * - Provides thread-safe access to session history
 * - Evicts sessions after TTL minutes of inactivity (scheduled)
 */
@Service
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    @Value("${session.ttl-minutes:60}")
    private int ttlMinutes;

    @Value("${session.max-history:50}")
    private int maxHistory;

    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();

    // ── Session creation ──────────────────────────────────────────────────────

    public String createSession(String agentId, String customerId, String intentStr) {
        Intent intent = parseIntent(intentStr);
        String sessionId = UUID.randomUUID().toString();
        ChatSession session = new ChatSession(sessionId, agentId, customerId, intent);
        sessions.put(sessionId, session);
        log.info("Session created: {} (agent={}, customer={}, intent={})",
            sessionId, agentId, customerId, intent);
        return sessionId;
    }

    // ── Session retrieval ─────────────────────────────────────────────────────

    public Optional<ChatSession> getSession(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public ChatSession getOrThrow(String sessionId) {
        return getSession(sessionId).orElseThrow(() ->
            new IllegalArgumentException("Session not found: " + sessionId));
    }

    public boolean sessionExists(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    // ── Session mutation ──────────────────────────────────────────────────────

    public void addUserMessage(String sessionId, String message) {
        ChatSession session = getOrThrow(sessionId);
        session.addUserMessage(message);
        session.trimHistory(maxHistory);
    }

    public void addAssistantMessage(String sessionId, String message) {
        ChatSession session = getOrThrow(sessionId);
        session.addAssistantMessage(message);
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("Session removed: {}", sessionId);
    }

    // ── TTL eviction — runs every 15 minutes ──────────────────────────────────

    @Scheduled(fixedDelay = 15 * 60 * 1000)
    public void evictExpiredSessions() {
        Instant cutoff = Instant.now().minus(ttlMinutes, ChronoUnit.MINUTES);
        int before = sessions.size();
        sessions.entrySet().removeIf(entry ->
            entry.getValue().getLastActivityAt().isBefore(cutoff)
        );
        int evicted = before - sessions.size();
        if (evicted > 0) {
            log.info("Evicted {} expired sessions (ttl={}min)", evicted, ttlMinutes);
        }
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Intent parseIntent(String intentStr) {
        if (intentStr == null) return Intent.GENERAL;
        try {
            return Intent.valueOf(intentStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown intent '{}', defaulting to GENERAL", intentStr);
            return Intent.GENERAL;
        }
    }
}
