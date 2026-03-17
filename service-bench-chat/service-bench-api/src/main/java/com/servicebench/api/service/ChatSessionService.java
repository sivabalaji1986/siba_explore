package com.servicebench.api.service;

import com.servicebench.api.client.OpsChatClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ChatSessionService {

    private static final Logger LOG = Logger.getLogger(ChatSessionService.class);

    @Inject
    @RestClient
    OpsChatClient opsChatClient;

    // Local registry: sessionId → intent, for quick lookup
    private final Map<String, String> sessionIntentMap = new ConcurrentHashMap<>();

    /**
     * Creates a session in the Ops Chat Service and registers it locally.
     */
    public String createSession(String agentId, String customerId, String intent) {
        LOG.infof("Creating session — agent=%s customer=%s intent=%s", agentId, customerId, intent);
        String sessionId = opsChatClient.createSession(agentId, customerId, intent);
        sessionIntentMap.put(sessionId, intent);
        LOG.infof("Session created: %s", sessionId);
        return sessionId;
    }

    /**
     * Fallback: generate a local session ID if Ops service is unavailable (dev mode).
     */
    public String createLocalSession(String agentId, String customerId, String intent) {
        String sessionId = UUID.randomUUID().toString();
        sessionIntentMap.put(sessionId, intent);
        return sessionId;
    }

    public String getIntent(String sessionId) {
        return sessionIntentMap.getOrDefault(sessionId, "GENERAL");
    }

    public boolean sessionExists(String sessionId) {
        return sessionIntentMap.containsKey(sessionId);
    }

    public void removeSession(String sessionId) {
        sessionIntentMap.remove(sessionId);
    }
}
