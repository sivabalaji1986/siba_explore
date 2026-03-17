import { LitElement, html, css } from 'lit';
import { customElement, state, query } from 'lit/decorators.js';
import { repeat } from 'lit/directives/repeat.js';
import {
  ChatMessage,
  ChatIntent,
  createSession,
  subscribeToChatStream
} from '../graphql-client';
import './chat-message';

@customElement('chat-panel')
export class ChatPanel extends LitElement {
  static styles = css`
    :host {
      display: flex;
      flex-direction: column;
      height: 100%;
      background: #0d1117;
      color: #e6edf3;
      font-family: 'IBM Plex Mono', 'Fira Code', monospace;
    }

    /* ── Header ── */
    .header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 20px;
      border-bottom: 1px solid #21262d;
      background: #161b22;
      flex-shrink: 0;
    }

    .header-left { display: flex; align-items: center; gap: 10px; }

    .status-dot {
      width: 8px; height: 8px;
      border-radius: 50%;
      background: #3fb950;
      box-shadow: 0 0 6px #3fb95088;
      flex-shrink: 0;
    }

    .status-dot.disconnected { background: #f85149; box-shadow: none; }
    .status-dot.connecting   { background: #d29922; box-shadow: 0 0 6px #d2992266; }

    .header-title {
      font-size: 13px;
      font-weight: 600;
      letter-spacing: 0.04em;
      color: #c9d1d9;
    }

    .session-id {
      font-size: 11px;
      color: #484f58;
      font-family: 'IBM Plex Mono', monospace;
    }

    /* ── Context bar ── */
    .context-bar {
      display: flex;
      gap: 8px;
      padding: 10px 20px;
      background: #0d1117;
      border-bottom: 1px solid #21262d;
      flex-shrink: 0;
      flex-wrap: wrap;
    }

    .context-field {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
    }

    .context-field label {
      color: #484f58;
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.06em;
    }

    .context-field input,
    .context-field select {
      background: #161b22;
      border: 1px solid #30363d;
      color: #c9d1d9;
      border-radius: 6px;
      padding: 4px 8px;
      font-size: 12px;
      font-family: inherit;
      outline: none;
      transition: border-color 0.15s;
    }

    .context-field input:focus,
    .context-field select:focus { border-color: #388bfd; }

    .intent-badge {
      padding: 3px 10px;
      border-radius: 20px;
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 0.04em;
      text-transform: uppercase;
    }

    .intent-ADDRESS_UPDATE  { background: #1f3a5c; color: #79b8ff; border: 1px solid #1f6feb; }
    .intent-ACCOUNT_ACTIVATION { background: #1a3a1a; color: #56d364; border: 1px solid #2ea043; }
    .intent-GENERAL         { background: #2d2a1f; color: #e3b341; border: 1px solid #9e6a03; }

    /* ── Messages ── */
    .messages {
      flex: 1;
      overflow-y: auto;
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 12px;
      scrollbar-width: thin;
      scrollbar-color: #30363d #0d1117;
    }

    .empty-state {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: #484f58;
      gap: 8px;
    }

    .empty-state .icon {
      font-size: 32px;
      opacity: 0.4;
    }

    .empty-state p {
      font-size: 13px;
      margin: 0;
    }

    /* ── Input bar ── */
    .input-bar {
      display: flex;
      gap: 8px;
      padding: 14px 20px;
      border-top: 1px solid #21262d;
      background: #161b22;
      flex-shrink: 0;
      align-items: flex-end;
    }

    .input-bar textarea {
      flex: 1;
      background: #0d1117;
      border: 1px solid #30363d;
      border-radius: 8px;
      color: #c9d1d9;
      font-family: inherit;
      font-size: 13px;
      padding: 10px 12px;
      resize: none;
      min-height: 40px;
      max-height: 120px;
      outline: none;
      transition: border-color 0.15s;
      line-height: 1.5;
    }

    .input-bar textarea:focus { border-color: #388bfd; }
    .input-bar textarea::placeholder { color: #484f58; }
    .input-bar textarea:disabled { opacity: 0.5; cursor: not-allowed; }

    .send-btn {
      background: #1f6feb;
      border: none;
      border-radius: 8px;
      color: #fff;
      cursor: pointer;
      font-size: 13px;
      font-weight: 600;
      font-family: inherit;
      padding: 10px 16px;
      transition: background 0.15s, opacity 0.15s;
      white-space: nowrap;
      flex-shrink: 0;
      height: 40px;
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .send-btn:hover:not(:disabled) { background: #388bfd; }
    .send-btn:disabled { opacity: 0.4; cursor: not-allowed; }

    .send-btn .spinner {
      width: 12px; height: 12px;
      border: 2px solid #ffffff44;
      border-top-color: #fff;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
    }

    @keyframes spin { to { transform: rotate(360deg); } }

    /* ── Error banner ── */
    .error-banner {
      padding: 8px 20px;
      background: #2d0e0e;
      border-bottom: 1px solid #f8514944;
      color: #f85149;
      font-size: 12px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .error-banner button {
      background: none;
      border: none;
      color: #f85149;
      cursor: pointer;
      font-size: 14px;
    }
  `;

  @state() private messages: ChatMessage[] = [];
  @state() private inputText = '';
  @state() private streaming = false;
  @state() private sessionId = '';
  @state() private agentId = 'agent-001';
  @state() private customerId = 'cust-001';
  @state() private intent: ChatIntent = 'ADDRESS_UPDATE';
  @state() private error = '';
  @state() private connectionStatus: 'connected' | 'disconnected' | 'connecting' = 'disconnected';

  @query('textarea') private textarea!: HTMLTextAreaElement;
  @query('.messages') private messagesContainer!: HTMLElement;

  private unsubscribe?: () => void;

  async connectedCallback() {
    super.connectedCallback();
    await this.initSession();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this.unsubscribe?.();
  }

  private async initSession() {
    this.connectionStatus = 'connecting';
    try {
      this.sessionId = await createSession(this.agentId, this.customerId, this.intent);
      this.connectionStatus = 'connected';
    } catch (e) {
      this.connectionStatus = 'disconnected';
      this.error = `Failed to create session: ${e}`;
    }
  }

  private async handleSend() {
    const text = this.inputText.trim();
    if (!text || this.streaming) return;

    this.error = '';
    this.inputText = '';

    // Add agent message
    const agentMsg: ChatMessage = {
      id: crypto.randomUUID(),
      role: 'agent',
      content: text,
      timestamp: new Date()
    };
    this.messages = [...this.messages, agentMsg];

    // Placeholder for streaming assistant response
    const assistantMsg: ChatMessage = {
      id: crypto.randomUUID(),
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      streaming: true
    };
    this.messages = [...this.messages, assistantMsg];
    this.streaming = true;

    this.scrollToBottom();

    this.unsubscribe = subscribeToChatStream(
      this.sessionId,
      text,
      this.intent,
      // onToken
      (token) => {
        this.messages = this.messages.map(m =>
          m.id === assistantMsg.id
            ? { ...m, content: m.content + token }
            : m
        );
        this.scrollToBottom();
      },
      // onComplete
      () => {
        this.messages = this.messages.map(m =>
          m.id === assistantMsg.id ? { ...m, streaming: false } : m
        );
        this.streaming = false;
      },
      // onError
      (err) => {
        this.error = `Stream error: ${err}`;
        this.messages = this.messages.map(m =>
          m.id === assistantMsg.id ? { ...m, streaming: false, content: m.content || '[error]' } : m
        );
        this.streaming = false;
      }
    );
  }

  private scrollToBottom() {
    this.updateComplete.then(() => {
      if (this.messagesContainer) {
        this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
      }
    });
  }

  private handleKeyDown(e: KeyboardEvent) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      this.handleSend();
    }
  }

  private handleTextareaInput(e: Event) {
    this.inputText = (e.target as HTMLTextAreaElement).value;
    const ta = e.target as HTMLTextAreaElement;
    ta.style.height = 'auto';
    ta.style.height = Math.min(ta.scrollHeight, 120) + 'px';
  }

  private async handleIntentChange(e: Event) {
    this.intent = (e.target as HTMLSelectElement).value as ChatIntent;
    this.messages = [];
    await this.initSession();
  }

  render() {
    const dotClass = `status-dot ${this.connectionStatus === 'connected' ? '' : this.connectionStatus}`;

    return html`
      <div class="header">
        <div class="header-left">
          <div class="${dotClass}"></div>
          <div>
            <div class="header-title">Service Bench — Ops Chat</div>
            <div class="session-id">${this.sessionId ? `session: ${this.sessionId.slice(0, 16)}…` : 'initialising…'}</div>
          </div>
        </div>
        <span class="intent-badge intent-${this.intent}">${this.intent.replace('_', ' ')}</span>
      </div>

      <div class="context-bar">
        <div class="context-field">
          <label>Agent</label>
          <input
            .value=${this.agentId}
            @change=${(e: Event) => { this.agentId = (e.target as HTMLInputElement).value; }}
            placeholder="agent-id"
          />
        </div>
        <div class="context-field">
          <label>Customer</label>
          <input
            .value=${this.customerId}
            @change=${(e: Event) => { this.customerId = (e.target as HTMLInputElement).value; }}
            placeholder="customer-id"
          />
        </div>
        <div class="context-field">
          <label>Intent</label>
          <select @change=${this.handleIntentChange}>
            <option value="ADDRESS_UPDATE" ?selected=${this.intent === 'ADDRESS_UPDATE'}>Address Update</option>
            <option value="ACCOUNT_ACTIVATION" ?selected=${this.intent === 'ACCOUNT_ACTIVATION'}>Account Activation</option>
            <option value="GENERAL" ?selected=${this.intent === 'GENERAL'}>General</option>
          </select>
        </div>
      </div>

      ${this.error ? html`
        <div class="error-banner">
          <span>${this.error}</span>
          <button @click=${() => this.error = ''}>×</button>
        </div>
      ` : ''}

      <div class="messages">
        ${this.messages.length === 0 ? html`
          <div class="empty-state">
            <div class="icon">◎</div>
            <p>Session ready. Type a message to begin.</p>
          </div>
        ` : repeat(
          this.messages,
          (m) => m.id,
          (m) => html`<chat-message .message=${m}></chat-message>`
        )}
      </div>

      <div class="input-bar">
        <textarea
          placeholder="Describe the customer's request… (Enter to send, Shift+Enter for newline)"
          .value=${this.inputText}
          @input=${this.handleTextareaInput}
          @keydown=${this.handleKeyDown}
          ?disabled=${this.streaming || !this.sessionId}
          rows="1"
        ></textarea>
        <button
          class="send-btn"
          @click=${this.handleSend}
          ?disabled=${this.streaming || !this.inputText.trim() || !this.sessionId}
        >
          ${this.streaming
            ? html`<span class="spinner"></span> Streaming`
            : html`Send ↑`}
        </button>
      </div>
    `;
  }
}
