# Service Bench — Ops Chat Solution

End-to-end chat system for customer care agents to raise static data update requests
(address update, account activation) on behalf of customers.

```
Service Bench UI (Lit/TS)
        ↕  GraphQL Subscription over SSE
Service Bench API (Quarkus + SmallRye GraphQL)
        ↕  HTTP POST + SSE stream
Ops Chat Service (Spring Boot + LangChain4j)
        ↕  Streaming API
LLM (Claude / GPT)
```

---

## Prerequisites

| Tool        | Version  |
|-------------|----------|
| Java        | 17+      |
| Maven       | 3.9+     |
| Node.js     | 18+      |
| npm         | 9+       |

---

## Project Structure

```
service-bench-ui/          ← Lit + TypeScript frontend (port 3000)
  src/
    graphql-client.ts      ← SSE subscription + mutation client
    components/
      chat-panel.ts        ← Main chat UI component
      chat-message.ts      ← Individual message bubble

service-bench-api/         ← Quarkus GraphQL API (port 8080)
  src/main/java/com/servicebench/api/
    graphql/
      ChatGraphQLResource.java   ← @Mutation + @Subscription
    client/
      OpsChatClient.java         ← REST client → Ops Chat Service
    service/
      ChatSessionService.java    ← Session registry
    model/
      ChatInput.java / ChatToken.java

ops-chat-service/          ← Spring Boot + LangChain4j (port 9090)
  src/main/java/com/ops/chat/
    controller/
      ChatController.java        ← POST /chat/session, GET /chat/stream
    service/
      SessionManager.java        ← Session lifecycle + TTL eviction
      ChatStreamingService.java  ← LLM streaming → SseEmitter
      PromptBuilder.java         ← Intent-specific system prompts
    model/
      ChatSession.java           ← Session + message history
    config/
      LlmConfig.java             ← Wires Claude or GPT
      CorsConfig.java
```

---

## Quick Start

### 1 — Ops Chat Service

```bash
cd ops-chat-service

# Set your LLM API key (choose one)
export ANTHROPIC_API_KEY=sk-ant-...
# OR
export OPENAI_API_KEY=sk-...

# If using OpenAI, also set in application.properties:
# llm.provider=openai

mvn spring-boot:run
# Listening on http://localhost:9090
```

Verify:
```bash
curl http://localhost:9090/chat/health
# {"status":"UP","activeSessions":0}
```

### 2 — Service Bench API (Quarkus)

```bash
cd service-bench-api
mvn quarkus:dev
# Listening on http://localhost:8080
# GraphQL UI: http://localhost:8080/q/graphql-ui
```

### 3 — Service Bench UI

```bash
cd service-bench-ui
npm install
npm run dev
# Open http://localhost:3000
```

---

## GraphQL Schema

```graphql
type Query {
  ping: String!
}

type Mutation {
  createChatSession(agentId: String!, customerId: String!, intent: String!): String!
  sendChatMessage(input: ChatInput!): String!
}

type Subscription {
  chatStream(sessionId: String!, message: String!, intent: String!): ChatToken!
}

input ChatInput {
  sessionId:  String!
  message:    String!
  intent:     String!
  agentId:    String!
  customerId: String!
}

type ChatToken {
  sessionId: String!
  token:     String!
  done:      Boolean!
}
```

---

## Ops Chat Service REST API

```
POST /chat/session
  Body (JSON): { "agentId": "...", "customerId": "...", "intent": "ADDRESS_UPDATE" }
  Returns: "session-uuid-string"

GET /chat/stream?sessionId=...&message=...&intent=...
  Returns: text/event-stream
  Events:
    event: token\ndata: <token>\n\n  (repeated per token)
    event: token\ndata: [DONE]\n\n   (terminal)
    event: error\ndata: <message>\n\n (on failure)

GET /chat/health
  Returns: { "status": "UP", "activeSessions": N }
```

---

## Intent System Prompts

| Intent               | Behaviour                                                      |
|----------------------|----------------------------------------------------------------|
| `ADDRESS_UPDATE`     | Collects name, account ID, old + new address, effective date   |
| `ACCOUNT_ACTIVATION` | Verifies identity, collects activation details                 |
| `GENERAL`            | Open-ended customer service assistance                         |

All intents guide the agent conversationally and produce a confirmation reference
(e.g. `REF-48291`) on completion.

---

## Session & State

- Sessions are created on the Ops Chat Service via `POST /chat/session`
- Full message history (user + assistant turns) is stored per session in memory
- History is passed to the LLM on every turn to maintain context
- Sessions are evicted after **60 minutes** of inactivity (configurable)
- History is capped at **50 messages** per session (configurable)

```properties
# ops-chat-service application.properties
session.ttl-minutes=60
session.max-history=50
```

---

## Switching Between Claude and GPT

In `ops-chat-service/src/main/resources/application.properties`:

```properties
# Use Claude (default)
llm.provider=claude
anthropic.api-key=${ANTHROPIC_API_KEY}
anthropic.model=claude-3-5-sonnet-20241022

# Use GPT
llm.provider=openai
openai.api-key=${OPENAI_API_KEY}
openai.model=gpt-4o
```

---

## End-to-End Token Flow

```
Agent types message
    │
    ▼
chat-panel.ts: subscribeToChatStream()
    │  GraphQL subscription (SSE) opened to /graphql
    ▼
ChatGraphQLResource.java: @Subscription chatStream()
    │  calls OpsChatClient.streamResponse() → GET /chat/stream
    ▼
ChatController.java: GET /chat/stream
    │  resolves session, spins virtual thread
    ▼
ChatStreamingService.java: llm.generate(history, handler)
    │  calls Anthropic/OpenAI streaming API
    ▼
StreamingResponseHandler.onNext(token)
    │  emitter.send(token)       → token arrives at Quarkus Multi
    │  Multi.map(ChatToken)      → SSE event to browser
    ▼
chat-panel.ts onToken callback
    │  appends token to message bubble
    ▼
[DONE] sentinel
    │  emitter.complete()
    │  Multi completes
    │  subscription closes
    ▼
Session history updated (full AI reply persisted)
```
