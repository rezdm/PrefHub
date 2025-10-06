import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  PlayerView,
  CreateGameRequest,
  JoinGameRequest,
  BidRequest,
  PlayCardRequest,
  ExchangeRequest,
  GameListItem,
  Card,
  Contract
} from '../types';

class ApiClient {
  private client: AxiosInstance;
  private token: string | null = null;

  constructor() {
    this.client = axios.create({
      baseURL: '/api',
      headers: {
        'Content-Type': 'application/json',
      },
      timeout: 10000,
    });

    // Request interceptor to add auth token
    this.client.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        if (this.token && config.headers) {
          config.headers.Authorization = `Bearer ${this.token}`;
        }
        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    // Response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          // Token expired or invalid
          this.clearToken();
          window.location.reload();
        }
        return Promise.reject(error);
      }
    );
  }

  setToken(token: string) {
    this.token = token;
    localStorage.setItem('auth_token', token);
  }

  getToken(): string | null {
    if (!this.token) {
      this.token = localStorage.getItem('auth_token');
    }
    return this.token;
  }

  clearToken() {
    this.token = null;
    localStorage.removeItem('auth_token');
  }

  // Auth endpoints
  async login(username: string, password: string): Promise<AuthResponse> {
    const request: LoginRequest = { username, password };
    const response = await this.client.post<{ token: string }>('/auth/login', request);
    const token = response.data.token;
    this.setToken(token);
    return { token, username };
  }

  async register(username: string, password: string): Promise<{ message: string }> {
    const request: RegisterRequest = { username, password };
    const response = await this.client.post<{ message: string }>('/auth/register', request);
    return response.data;
  }

  async logout(): Promise<void> {
    try {
      await this.client.post('/auth/logout');
    } finally {
      this.clearToken();
    }
  }

  // Game endpoints
  async createGame(gameId: string, ruleId: string = 'CLASSIC'): Promise<PlayerView> {
    const request: CreateGameRequest = { gameId, ruleId };
    const response = await this.client.post<PlayerView>('/games/create', request);
    return response.data;
  }

  async joinGame(gameId: string): Promise<PlayerView> {
    const request: JoinGameRequest = { gameId };
    const response = await this.client.post<PlayerView>('/games/join', request);
    return response.data;
  }

  async listGames(): Promise<GameListItem[]> {
    const response = await this.client.get<GameListItem[]>('/games/list');
    return response.data;
  }

  async getGameState(gameId: string): Promise<PlayerView> {
    const response = await this.client.get<PlayerView>('/games/state', {
      params: { gameId }
    });
    return response.data;
  }

  async placeBid(gameId: string, contract: Contract): Promise<PlayerView> {
    const request: BidRequest = { gameId, contract };
    const response = await this.client.post<PlayerView>('/games/bid', request);
    return response.data;
  }

  async getAvailableBids(gameId: string): Promise<Contract[]> {
    const response = await this.client.get<Contract[]>('/games/available-bids', {
      params: { gameId }
    });
    return response.data;
  }

  async exchangeWidow(gameId: string, cards: Card[]): Promise<PlayerView> {
    const request: ExchangeRequest = { gameId, cards };
    const response = await this.client.post<PlayerView>('/games/exchange', request);
    return response.data;
  }

  async playCard(gameId: string, card: Card): Promise<PlayerView> {
    const request: PlayCardRequest = { gameId, card };
    const response = await this.client.post<PlayerView>('/games/play', request);
    return response.data;
  }

  async startNextRound(gameId: string): Promise<PlayerView> {
    const response = await this.client.post<PlayerView>('/games/next-round', { gameId });
    return response.data;
  }
}

// Export singleton instance
export const apiClient = new ApiClient();
export default apiClient;
