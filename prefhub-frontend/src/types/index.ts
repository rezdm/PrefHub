// Card models
export type Suit = 'SPADES' | 'CLUBS' | 'DIAMONDS' | 'HEARTS';

export type Rank = 'SEVEN' | 'EIGHT' | 'NINE' | 'TEN' | 'JACK' | 'QUEEN' | 'KING' | 'ACE';

export interface Card {
  suit: Suit;
  rank: Rank;
}

// Game phase enum
export type GamePhase =
  | 'WAITING_FOR_PLAYERS'
  | 'BIDDING'
  | 'WIDOW_EXCHANGE'
  | 'PLAYING'
  | 'ROUND_COMPLETE';

// Contract types
export type Contract =
  | 'SIX_SPADES' | 'SIX_CLUBS' | 'SIX_DIAMONDS' | 'SIX_HEARTS'
  | 'SEVEN_SPADES' | 'SEVEN_CLUBS' | 'SEVEN_DIAMONDS' | 'SEVEN_HEARTS'
  | 'EIGHT_SPADES' | 'EIGHT_CLUBS' | 'EIGHT_DIAMONDS' | 'EIGHT_HEARTS'
  | 'NINE_SPADES' | 'NINE_CLUBS' | 'NINE_DIAMONDS' | 'NINE_HEARTS'
  | 'TEN_SPADES' | 'TEN_CLUBS' | 'TEN_DIAMONDS' | 'TEN_HEARTS'
  | 'MISER' | 'PASS';

// Game Rules
export interface GameRules {
  id: string;
  name: string;
  minPlayers: number;
  maxPlayers: number;
  initialPool: number;
  mountainLimit: number;
  bulletCost: number;
}

// Player View - what each player sees
export interface PlayerView {
  gameId: string;
  playerUsername: string;
  phase: GamePhase;
  roundNumber: number;
  hand: Card[];
  otherPlayers: string[];
  currentPlayerUsername: string;
  isYourTurn: boolean;
  allowedActions: string[];
  nextActionDescription: string;
  bids: Record<string, string>;
  highestBid: Contract | null;
  widow: Card[];
  contract: Contract | null;
  declarerUsername: string | null;
  trumpSuit: Suit | null;
  currentTrick: Record<string, Card>;
  tricksWon: Record<string, number>;
  scores: Record<string, number>;
  bullets: Record<string, number>;
  mountains: Record<string, number>;
  rules: GameRules;
}

// Game State (simplified version)
export interface GameState {
  gameId: string;
  phase: GamePhase;
  players: string[];
  currentPlayer: string;
}

// Auth models
export interface AuthResponse {
  token: string;
  username?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
}

// API Request/Response types
export interface CreateGameRequest {
  gameId: string;
  ruleId: string;
}

export interface JoinGameRequest {
  gameId: string;
}

export interface BidRequest {
  gameId: string;
  contract: string;
}

export interface PlayCardRequest {
  gameId: string;
  card: Card;
}

export interface ExchangeRequest {
  gameId: string;
  cards: Card[];
}

// Game List Item
export interface GameListItem {
  gameId: string;
  players: string[];
  phase: GamePhase;
  ruleId: string;
}

// Helper functions for cards
export const getSuitSymbol = (suit: Suit): string => {
  const symbols: Record<Suit, string> = {
    SPADES: '♠',
    CLUBS: '♣',
    DIAMONDS: '♦',
    HEARTS: '♥'
  };
  return symbols[suit];
};

export const getRankSymbol = (rank: Rank): string => {
  const symbols: Record<Rank, string> = {
    SEVEN: '7',
    EIGHT: '8',
    NINE: '9',
    TEN: '10',
    JACK: 'В',
    QUEEN: 'Д',
    KING: 'К',
    ACE: 'Т'
  };
  return symbols[rank];
};

export const getSuitColor = (suit: Suit): string => {
  return (suit === 'DIAMONDS' || suit === 'HEARTS') ? 'red' : 'black';
};

export const cardToString = (card: Card): string => {
  return `${getRankSymbol(card.rank)}${getSuitSymbol(card.suit)}`;
};

// Sort cards by suit order (Spades, Clubs, Diamonds, Hearts) then by rank within each suit
export const sortCards = (cards: Card[]): Card[] => {
  const suitOrder: Record<Suit, number> = {
    SPADES: 0,
    CLUBS: 1,
    DIAMONDS: 2,
    HEARTS: 3
  };

  const rankOrder: Record<Rank, number> = {
    SEVEN: 0,
    EIGHT: 1,
    NINE: 2,
    TEN: 3,
    JACK: 4,
    QUEEN: 5,
    KING: 6,
    ACE: 7
  };

  return [...cards].sort((a, b) => {
    const suitDiff = suitOrder[a.suit] - suitOrder[b.suit];
    if (suitDiff !== 0) {
      return suitDiff;
    }
    return rankOrder[a.rank] - rankOrder[b.rank];
  });
};
