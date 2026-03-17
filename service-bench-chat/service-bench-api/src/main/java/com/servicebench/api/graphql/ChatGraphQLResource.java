package com.servicebench.api.graphql;

import com.servicebench.api.client.OpsChatClient;
import com.servicebench.api.model.ChatInput;
import com.servicebench.api.model.ChatToken;
import com.servicebench.api.service.ChatSessionService;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Subscription;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@GraphQLApi
public class ChatGraphQLResource {

    private static final Logger LOG = Logger.getLogger(ChatGraphQLResource.class);

    @Inject
    ChatSessionService sessionService;

    @Inject
    @RestClient
    OpsChatClient opsChatClient;

    // ── Health check query ────────────────────────────────────────────────────

    @Query("ping")
    @Description("Health check")
    public String ping() {
        return "Service Bench API is running";
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * Creates a new chat session in the Ops Chat Service.
     * Returns the sessionId to the UI.
     */
    @Mutation("createChatSession")
    @Description("Create a new chat session for a customer care agent")
    public String createChatSession(
        @Name("agentId")    String agentId,
        @Name("customerId") String customerId,
        @Name("intent")     String intent
    ) {
        LOG.infof("createChatSession: agent=%s customer=%s intent=%s", agentId, customerId, intent);
        return sessionService.createSession(agentId, customerId, intent);
    }

    /**
     * Persists a chat message (optional — the streaming subscription handles
     * forwarding the message; this mutation can be used for audit logging).
     */
    @Mutation("sendChatMessage")
    @Description("Persist a chat message for audit. Returns the sessionId.")
    public String sendChatMessage(@Name("input") ChatInput input) {
        LOG.infof("sendChatMessage: session=%s intent=%s", input.sessionId, input.intent);
        // Extend here: persist to a DB, emit an audit event, etc.
        return input.sessionId;
    }

    // ── Subscription ──────────────────────────────────────────────────────────

    /**
     * GraphQL Subscription — delivered over SSE by Quarkus SmallRye GraphQL.
     *
     * Flow:
     *   1. UI opens this subscription with (sessionId, message, intent)
     *   2. This method calls the Ops Chat Service SSE stream endpoint
     *   3. The Ops Chat Service calls the LLM (Claude/GPT via LangChain4j)
     *   4. Each token is emitted as a ChatToken back through the Multi pipeline
     *   5. Quarkus propagates each ChatToken as an SSE event to the UI
     *
     * The Multi is fully non-blocking end-to-end — no thread is held.
     */
    @Subscription("chatStream")
    @Description("Stream LLM response tokens for a chat message")
    public Multi<ChatToken> chatStream(
        @Name("sessionId") String sessionId,
        @Name("message")   String message,
        @Name("intent")    String intent
    ) {
        LOG.debugf("chatStream subscription opened: session=%s intent=%s", sessionId, intent);

        return opsChatClient
            .streamResponse(sessionId, message, intent)
            .map(rawToken -> {
                // The Ops service sends "[DONE]" as a terminal signal
                boolean done = "[DONE]".equals(rawToken);
                return new ChatToken(sessionId, done ? "" : rawToken, done);
            })
            .onFailure().invoke(err ->
                LOG.errorf("Stream error for session %s: %s", sessionId, err.getMessage())
            )
            .onCompletion().invoke(() ->
                LOG.debugf("Stream complete for session %s", sessionId)
            );
    }
}
