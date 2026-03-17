package com.ops.chat.model;

public class SessionRequest {
    private String agentId;
    private String customerId;
    private String intent;

    public SessionRequest() {}

    public String getAgentId()    { return agentId; }
    public String getCustomerId() { return customerId; }
    public String getIntent()     { return intent; }

    public void setAgentId(String agentId)       { this.agentId = agentId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setIntent(String intent)         { this.intent = intent; }
}
