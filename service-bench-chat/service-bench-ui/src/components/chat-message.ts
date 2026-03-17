import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { ChatMessage } from '../graphql-client';

@customElement('chat-message')
export class ChatMessageElement extends LitElement {
  static styles = css`
    :host { display: block; }

    .message {
      display: flex;
      gap: 12px;
      padding: 4px 0;
      animation: fadeIn 0.2s ease;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(6px); }
      to   { opacity: 1; transform: translateY(0); }
    }

    .message.agent  { flex-direction: row-reverse; }

    .avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      font-weight: 600;
      flex-shrink: 0;
      margin-top: 2px;
    }

    .message.agent .avatar {
      background: #2a3a5c;
      color: #a8c4f0;
    }

    .message.assistant .avatar {
      background: #1a2e1a;
      color: #6fcf97;
    }

    .bubble {
      max-width: 72%;
      padding: 10px 14px;
      border-radius: 14px;
      font-size: 14px;
      line-height: 1.55;
      white-space: pre-wrap;
      word-break: break-word;
    }

    .message.agent .bubble {
      background: #1e3a6e;
      color: #dce8ff;
      border-bottom-right-radius: 4px;
    }

    .message.assistant .bubble {
      background: #1a2820;
      color: #d4edda;
      border-bottom-left-radius: 4px;
      border: 1px solid #2d4a32;
    }

    .cursor {
      display: inline-block;
      width: 2px;
      height: 14px;
      background: #6fcf97;
      margin-left: 2px;
      vertical-align: text-bottom;
      animation: blink 0.8s step-end infinite;
    }

    @keyframes blink {
      50% { opacity: 0; }
    }

    .meta {
      font-size: 11px;
      color: #4a5568;
      margin-top: 4px;
      padding: 0 4px;
    }

    .message.agent .meta  { text-align: right; }
    .message.assistant .meta { text-align: left; }
  `;

  @property({ type: Object }) message!: ChatMessage;

  render() {
    const { role, content, timestamp, streaming } = this.message;
    const initials = role === 'agent' ? 'AG' : 'AI';
    const timeStr = timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    return html`
      <div class="message ${role}">
        <div class="avatar">${initials}</div>
        <div>
          <div class="bubble">
            ${content}${streaming ? html`<span class="cursor"></span>` : ''}
          </div>
          <div class="meta">${timeStr}</div>
        </div>
      </div>
    `;
  }
}
