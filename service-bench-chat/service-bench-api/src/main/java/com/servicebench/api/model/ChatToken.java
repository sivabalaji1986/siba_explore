package com.servicebench.api.model;

public class ChatToken {

    private String sessionId;
    private String token;
    private boolean done;

    public ChatToken() {}

    public ChatToken(String sessionId, String token, boolean done) {
        this.sessionId = sessionId;
        this.token     = token;
        this.done      = done;
    }

    public String getSessionId() { return sessionId; }
    public String getToken()     { return token; }
    public boolean isDone()      { return done; }

    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setToken(String token)         { this.token = token; }
    public void setDone(boolean done)          { this.done = done; }
}
