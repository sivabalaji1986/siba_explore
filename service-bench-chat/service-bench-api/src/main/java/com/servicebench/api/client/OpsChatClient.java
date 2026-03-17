package com.servicebench.api.client;

import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client that proxies calls to the Ops Chat Service.
 *
 * The Ops Chat Service exposes:
 *   POST /chat/session          — create a new session
 *   GET  /chat/stream           — stream tokens via SSE
 */
@RegisterRestClient(configKey = "ops-chat")
@Path("/chat")
public interface OpsChatClient {

    /**
     * Create a new chat session in the Ops Chat Service.
     * Returns the sessionId.
     */
    @POST
    @Path("/session")
    @Produces(MediaType.APPLICATION_JSON)
    String createSession(
        @QueryParam("agentId")    String agentId,
        @QueryParam("customerId") String customerId,
        @QueryParam("intent")     String intent
    );

    /**
     * Open an SSE stream for a given session/message.
     * Quarkus REST Client Reactive propagates this as a Mutiny Multi
     * which the @Subscription method pipes directly back to the UI.
     */
    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    Multi<String> streamResponse(
        @QueryParam("sessionId") String sessionId,
        @QueryParam("message")   String message,
        @QueryParam("intent")    String intent
    );
}
