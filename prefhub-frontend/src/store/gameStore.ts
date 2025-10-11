import { create } from 'zustand';
import { apiClient } from '../api/client';
import { wsClient } from '../api/websocket';
import type { PlayerView, Card, Contract } from '../types';

interface GameStore {
  // Auth state
  token: string | null;
  username: string | null;
  isAuthenticated: boolean;

  // Game state
  currentGameId: string | null;
  gameState: PlayerView | null;
  error: string | null;
  loading: boolean;
  wsConnected: boolean;

  // Auth actions
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, password: string) => Promise<void>;
  logout: () => void;
  initializeAuth: () => void;

  // Game actions
  createGame: (gameId: string, ruleId?: string) => Promise<void>;
  joinGame: (gameId: string) => Promise<void>;
  leaveGame: () => void;
  refreshGameState: () => Promise<void>;
  placeBid: (contract: Contract) => Promise<void>;
  playCard: (card: Card) => Promise<void>;
  exchangeWidow: (cards: Card[]) => Promise<void>;
  startNextRound: () => Promise<void>;

  // Utility actions
  setError: (error: string | null) => void;
  clearError: () => void;
}

// Setup WebSocket message handlers
const setupWebSocketHandlers = (set: any, get: any) => {
  wsClient.on('gameState', (data: any) => {
    console.log('[WebSocket] Game state update received');
    set({ gameState: data.state, error: null });
  });

  wsClient.on('error', (data: any) => {
    console.error('[WebSocket] Error:', data.message);
    set({ error: data.message });
  });
};

export const useGameStore = create<GameStore>((set, get) => ({
  // Initial state
  token: null,
  username: null,
  isAuthenticated: false,
  currentGameId: null,
  gameState: null,
  error: null,
  loading: false,
  wsConnected: false,

  // Auth actions
  initializeAuth: () => {
    const token = apiClient.getToken();
    const username = localStorage.getItem('username');
    console.log('[initializeAuth] token:', token, 'username:', username);
    if (token && username) {
      set({ token, username, isAuthenticated: true });
      console.log('[initializeAuth] Auth restored');

      // Connect WebSocket if we have a token
      try {
        setupWebSocketHandlers(set, get);
        wsClient.connect(token).then(() => {
          set({ wsConnected: true });
          console.log('[initializeAuth] WebSocket connected');
        }).catch((wsError) => {
          console.error('[initializeAuth] WebSocket connection failed:', wsError);
          // Don't fail auth restore if WebSocket fails
        });
      } catch (error) {
        console.error('[initializeAuth] Error setting up WebSocket:', error);
      }
    } else {
      console.log('[initializeAuth] No auth to restore');
    }
  },

  login: async (username: string, password: string) => {
    try {
      console.log('[login] Starting login for:', username);
      set({ loading: true, error: null });
      const response = await apiClient.login(username, password);
      console.log('[login] API response:', response);
      localStorage.setItem('username', username);
      set({
        token: response.token,
        username,
        isAuthenticated: true,
        loading: false,
      });
      console.log('[login] State updated, isAuthenticated: true');

      // Connect WebSocket
      try {
        setupWebSocketHandlers(set, get);
        await wsClient.connect(response.token);
        set({ wsConnected: true });
        console.log('[login] WebSocket connected');
      } catch (wsError) {
        console.error('[login] WebSocket connection failed:', wsError);
        // Don't fail login if WebSocket fails, will retry automatically
      }
    } catch (error: any) {
      console.error('[login] Error:', error);
      const errorMessage = error.response?.data?.message || 'Login failed';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  register: async (username: string, password: string) => {
    try {
      set({ loading: true, error: null });
      await apiClient.register(username, password);
      set({ loading: false });
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Registration failed';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  logout: () => {
    apiClient.logout();
    wsClient.disconnect();
    localStorage.removeItem('username');
    set({
      token: null,
      username: null,
      isAuthenticated: false,
      currentGameId: null,
      gameState: null,
      error: null,
      wsConnected: false,
    });
  },

  // Game actions
  createGame: async (gameId: string, ruleId: string = 'CLASSIC') => {
    try {
      set({ loading: true, error: null });
      const gameState = await apiClient.createGame(gameId, ruleId);
      set({
        currentGameId: gameId,
        gameState,
        loading: false,
      });
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Failed to create game';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  joinGame: async (gameId: string) => {
    try {
      set({ loading: true, error: null });

      // Use API for initial join (creates player in game)
      const gameState = await apiClient.joinGame(gameId);
      set({
        currentGameId: gameId,
        gameState,
        loading: false,
      });

      // Then join via WebSocket for real-time updates
      if (get().wsConnected) {
        wsClient.joinGame(gameId);
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Failed to join game';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  leaveGame: () => {
    set({
      currentGameId: null,
      gameState: null,
    });
  },

  refreshGameState: async () => {
    const { currentGameId } = get();
    if (!currentGameId) return;

    try {
      const gameState = await apiClient.getGameState(currentGameId);
      set({ gameState, error: null });
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Failed to refresh game state';
      set({ error: errorMessage });
      throw error;
    }
  },

  placeBid: async (contract: Contract) => {
    const { currentGameId, wsConnected } = get();
    if (!currentGameId) return;

    try {
      set({ loading: true, error: null });

      if (wsConnected) {
        // Use WebSocket for real-time
        wsClient.placeBid(contract);
        set({ loading: false });
      } else {
        // Fallback to API
        const gameState = await apiClient.placeBid(currentGameId, contract);
        set({ gameState, loading: false });
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Failed to place bid';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  playCard: async (card: Card) => {
    const { currentGameId, wsConnected } = get();
    if (!currentGameId) return;

    try {
      set({ loading: true, error: null });

      if (wsConnected) {
        // Use WebSocket for real-time
        wsClient.playCard(card);
        set({ loading: false });
      } else {
        // Fallback to API
        const gameState = await apiClient.playCard(currentGameId, card);
        set({ gameState, loading: false });
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Failed to play card';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  exchangeWidow: async (cards: Card[]) => {
    const { currentGameId, wsConnected } = get();
    if (!currentGameId) return;

    try {
      set({ loading: true, error: null });

      if (wsConnected) {
        // Use WebSocket for real-time
        wsClient.exchangeWidow(cards);
        set({ loading: false });
      } else {
        // Fallback to API
        const gameState = await apiClient.exchangeWidow(currentGameId, cards);
        set({ gameState, loading: false });
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Failed to exchange widow';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  startNextRound: async () => {
    const { currentGameId, wsConnected } = get();
    if (!currentGameId) return;

    try {
      set({ loading: true, error: null });

      if (wsConnected) {
        // Use WebSocket for real-time
        wsClient.startNextRound();
        set({ loading: false });
      } else {
        // Fallback to API
        const gameState = await apiClient.startNextRound(currentGameId);
        set({ gameState, loading: false });
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Failed to start next round';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  // Utility actions
  setError: (error: string | null) => {
    set({ error });
  },

  clearError: () => {
    set({ error: null });
  },
}));

export default useGameStore;
