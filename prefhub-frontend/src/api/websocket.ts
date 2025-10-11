import type { PlayerView, Card, Contract } from '../types';

type MessageHandler = (data: any) => void;

interface WebSocketMessage {
  type: string;
  [key: string]: any;
}

export class GameWebSocketClient {
  private ws: WebSocket | null = null;
  private token: string | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000;
  private messageHandlers: Map<string, Set<MessageHandler>> = new Map();
  private isConnecting = false;

  constructor(private url: string = 'ws://localhost:8091') {}

  connect(token: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        resolve();
        return;
      }

      if (this.isConnecting) {
        reject(new Error('Connection already in progress'));
        return;
      }

      this.isConnecting = true;
      this.token = token;

      try {
        this.ws = new WebSocket(this.url);

        this.ws.onopen = () => {
          console.log('[WebSocket] Connected');
          this.isConnecting = false;
          this.reconnectAttempts = 0;

          // Authenticate
          this.send({ type: 'auth', token });

          // Wait for auth success
          const authHandler = (data: any) => {
            if (data.type === 'authSuccess') {
              console.log('[WebSocket] Authenticated as', data.username);
              this.off('authSuccess', authHandler);
              this.off('authError', authErrorHandler);
              resolve();
            }
          };

          const authErrorHandler = (data: any) => {
            if (data.type === 'authError') {
              console.error('[WebSocket] Auth failed:', data.message);
              this.off('authSuccess', authHandler);
              this.off('authError', authErrorHandler);
              this.disconnect();
              reject(new Error(data.message));
            }
          };

          this.on('authSuccess', authHandler);
          this.on('authError', authErrorHandler);
        };

        this.ws.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            console.log('[WebSocket] Received:', data.type);
            this.handleMessage(data);
          } catch (error) {
            console.error('[WebSocket] Failed to parse message:', error);
          }
        };

        this.ws.onerror = (error) => {
          console.error('[WebSocket] Error:', error);
          this.isConnecting = false;
          reject(error);
        };

        this.ws.onclose = () => {
          console.log('[WebSocket] Disconnected');
          this.isConnecting = false;
          this.attemptReconnect();
        };
      } catch (error) {
        this.isConnecting = false;
        reject(error);
      }
    });
  }

  disconnect() {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    this.token = null;
    this.reconnectAttempts = 0;
  }

  private attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts || !this.token) {
      console.log('[WebSocket] Max reconnect attempts reached or no token');
      return;
    }

    this.reconnectAttempts++;
    const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);

    console.log(`[WebSocket] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`);

    setTimeout(() => {
      if (this.token) {
        this.connect(this.token).catch(console.error);
      }
    }, delay);
  }

  send(message: WebSocketMessage) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      console.warn('[WebSocket] Cannot send, not connected');
    }
  }

  on(type: string, handler: MessageHandler) {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, new Set());
    }
    this.messageHandlers.get(type)!.add(handler);
  }

  off(type: string, handler: MessageHandler) {
    this.messageHandlers.get(type)?.delete(handler);
  }

  private handleMessage(data: any) {
    const handlers = this.messageHandlers.get(data.type);
    if (handlers) {
      handlers.forEach(handler => handler(data));
    }

    // Also call wildcard handlers
    const wildcardHandlers = this.messageHandlers.get('*');
    if (wildcardHandlers) {
      wildcardHandlers.forEach(handler => handler(data));
    }
  }

  // Game actions
  joinGame(gameId: string) {
    this.send({ type: 'join', gameId });
  }

  getState() {
    this.send({ type: 'getState' });
  }

  placeBid(contract: Contract) {
    this.send({ type: 'placeBid', contract });
  }

  exchangeWidow(cards: Card[]) {
    this.send({ type: 'exchangeWidow', cards });
  }

  playCard(card: Card) {
    this.send({ type: 'playCard', card });
  }

  startNextRound() {
    this.send({ type: 'startNextRound' });
  }
}

export const wsClient = new GameWebSocketClient();
