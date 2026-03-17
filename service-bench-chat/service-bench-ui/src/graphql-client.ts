import { createClient, Client } from 'graphql-sse';

export interface ChatToken {
  sessionId: string;
  token: string;
  done: boolean;
}

export interface ChatMessage {
  id: string;
  role: 'agent' | 'assistant';
  content: string;
  timestamp: Date;
  streaming?: boolean;
}

export type ChatIntent = 'ADDRESS_UPDATE' | 'ACCOUNT_ACTIVATION' | 'GENERAL';

const GRAPHQL_URL = '/graphql';

// ── SSE subscription client ─────────────────────────────────────────────────
const sseClient: Client = createClient({
  url: GRAPHQL_URL,
  headers: { Accept: 'text/event-stream' }
});

// ── Plain HTTP client for mutations/queries ──────────────────────────────────
async function gqlFetch<T>(query: string, variables?: Record<string, unknown>): Promise<T> {
  const res = await fetch(GRAPHQL_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query, variables })
  });
  if (!res.ok) throw new Error(`GraphQL HTTP error: ${res.status}`);
  const json = await res.json();
  if (json.errors?.length) throw new Error(json.errors[0].message);
  return json.data as T;
}

// ── Mutations ────────────────────────────────────────────────────────────────
export async function sendChatMessage(
  sessionId: string,
  message: string,
  intent: ChatIntent,
  agentId: string,
  customerId: string
): Promise<string> {
  const data = await gqlFetch<{ sendChatMessage: string }>(
    `mutation SendChat($input: ChatInput!) {
       sendChatMessage(input: $input)
     }`,
    {
      input: { sessionId, message, intent, agentId, customerId }
    }
  );
  return data.sendChatMessage;
}

export async function createSession(
  agentId: string,
  customerId: string,
  intent: ChatIntent
): Promise<string> {
  const data = await gqlFetch<{ createChatSession: string }>(
    `mutation CreateSession($agentId: String!, $customerId: String!, $intent: String!) {
       createChatSession(agentId: $agentId, customerId: $customerId, intent: $intent)
     }`,
    { agentId, customerId, intent }
  );
  return data.createChatSession;
}

// ── Subscription ─────────────────────────────────────────────────────────────
export function subscribeToChatStream(
  sessionId: string,
  message: string,
  intent: ChatIntent,
  onToken: (token: string) => void,
  onComplete: () => void,
  onError: (err: unknown) => void
): () => void {
  const unsubscribe = sseClient.subscribe<{ chatStream: ChatToken }>(
    {
      query: `subscription ChatStream($sessionId: String!, $message: String!, $intent: String!) {
                chatStream(sessionId: $sessionId, message: $message, intent: $intent) {
                  sessionId
                  token
                  done
                }
              }`,
      variables: { sessionId, message, intent }
    },
    {
      next: ({ data }) => {
        if (!data) return;
        const { token, done } = data.chatStream;
        if (done) {
          onComplete();
        } else {
          onToken(token);
        }
      },
      error: onError,
      complete: onComplete
    }
  );
  return unsubscribe;
}
