package com.ops.chat.controller;

import com.ops.chat.model.SessionRequest;
import com.ops.chat.service.ChatStreamingService;
import com.ops.chat.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Exposes two endpoints consumed by the Service Bench API (Quarkus):
 *
 *   POST /chat/session      — create a session, returns sessionId (plain text)
 *   GET  /chat/stream       — open SSE stream for a message turn
 *   GET  /chat/health       — health check
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    // SSE timeout: 5 minutes per request max
    private static final long SSE_TIMEOUT_MS = 5 * 60 * 1000L;

    private final SessionManager sessionManager;
    private final ChatStreamingService streamingService;

    public ChatController(SessionManager sessionManager, ChatStreamingService streamingService) {
        this.sessionManager   = sessionManager;
        this.streamingService = streamingService;
    }

    // ── POST /chat/session ────────────────────────────────────────────────────

    /**
     * Creates a new session. Called by Service Bench API when the agent opens a chat.
     *
     * Can be called with query params OR a JSON body — both are supported
     * to make it easy to test from Quarkus REST client and curl.
     */
    @PostMapping(value = "/session", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createSession(
        @RequestParam(required = false) String agentId,
        @RequestParam(required = false) String customerId,
        @RequestParam(required = false) String intent,
        @RequestBody(required = false)  SessionRequest body
    ) {
        // Prefer body over query params
        String resolvedAgentId    = body != null && body.getAgentId()    != null ? body.getAgentId()    : agentId;
        String resolvedCustomerId = body != null && body.getCustomerId() != null ? body.getCustomerId() : customerId;
        String resolvedIntent     = body != null && body.getIntent()     != null ? body.getIntent()     : intent;

        if (resolvedAgentId == null || resolvedCustomerId == null) {
            return ResponseEntity.badRequest().body("\"agentId and customerId are required\"");
        }

        String sessionId = sessionManager.createSession(
            resolvedAgentId,
            resolvedCustomerId,
            resolvedIntent != null ? resolvedIntent : "GENERAL"
        );

        log.info("Session created via API: {}", sessionId);
        return ResponseEntity.ok("\"" + sessionId + "\""); // JSON string
    }

    // ── GET /chat/stream ──────────────────────────────────────────────────────

    /**
     * Opens an SSE stream for one message turn.
     *
     * The Quarkus OpsChatClient consumes this as a Multi<String>.
     * Each SSE event is a raw token string; the final event is "[DONE]".
     *
     * Headers set on the response to ensure proper SSE behaviour:
     *   Content-Type: text/event-stream
     *   Cache-Control: no-cache
     *   X-Accel-Buffering: no  (disables nginx proxy buffering)
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
        @RequestParam String sessionId,
        @RequestParam String message,
        @RequestParam(defaultValue = "GENERAL") String intent
    ) {
        log.info("Stream request: session={} intent={} message='{}'",
            sessionId, intent, truncate(message, 60));

        if (!sessionManager.sessionExists(sessionId)) {
            log.warn("Session not found: {}", sessionId);
            SseEmitter errorEmitter = new SseEmitter(SSE_TIMEOUT_MS);
            try {
                errorEmitter.send(SseEmitter.event().name("error").data("Session not found: " + sessionId));
                errorEmitter.send(SseEmitter.event().name("token").data("[DONE]"));
                errorEmitter.complete();
            } catch (Exception e) {
                errorEmitter.completeWithError(e);
            }
            return errorEmitter;
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        // Run in a virtual thread to avoid blocking the request thread
        Thread.ofVirtual().start(() ->
            streamingService.streamToEmitter(sessionId, message, intent, emitter)
        );

        return emitter;
    }

    // ── GET /chat/health ──────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "activeSessions", sessionManager.getActiveSessionCount()
        ));
    }

    // ── Error handling ────────────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
