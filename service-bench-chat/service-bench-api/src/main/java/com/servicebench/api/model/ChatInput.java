package com.servicebench.api.model;

import org.eclipse.microprofile.graphql.Input;

@Input("ChatInput")
public class ChatInput {

    public String sessionId;
    public String message;
    public String intent;   // ADDRESS_UPDATE | ACCOUNT_ACTIVATION | GENERAL
    public String agentId;
    public String customerId;

    public ChatInput() {}

    public ChatInput(String sessionId, String message, String intent,
                     String agentId, String customerId) {
        this.sessionId  = sessionId;
        this.message    = message;
        this.intent     = intent;
        this.agentId    = agentId;
        this.customerId = customerId;
    }
}
