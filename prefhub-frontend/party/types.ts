// Shared types for PartyKit server and client

export type Suit = 'SPADES' | 'CLUBS' | 'DIAMONDS' | 'HEARTS';
export type Rank = 'SEVEN' | 'EIGHT' | 'NINE' | 'TEN' | 'JACK' | 'QUEEN' | 'KING' | 'ACE';

export interface Card {
  suit: Suit;
  rank: Rank;
}

export type GamePhase =
  | 'WAITING_FOR_PLAYERS'
  | 'BIDDING'
  | 'WIDOW_EXCHANGE'
  | 'PLAYING'
  | 'ROUND_COMPLETE';

export type Contract =
  | 'SIX_SPADES' | 'SIX_CLUBS' | 'SIX_DIAMONDS' | 'SIX_HEARTS'
  | 'SEVEN_SPADES' | 'SEVEN_CLUBS' | 'SEVEN_DIAMONDS' | 'SEVEN_HEARTS'
  | 'EIGHT_SPADES' | 'EIGHT_CLUBS' | 'EIGHT_DIAMONDS' | 'EIGHT_HEARTS'
  | 'NINE_SPADES' | 'NINE_CLUBS' | 'NINE_DIAMONDS' | 'NINE_HEARTS'
  | 'TEN_SPADES' | 'TEN_CLUBS' | 'TEN_DIAMONDS' | 'TEN_HEARTS'
  | 'MISER' | 'PASS';

export interface GameRules {
  id: string;
  name: string;
  minPlayers: number;
  maxPlayers: number;
  initialPool: number;
  mountainLimit: number;
  bulletCost: number;
}

export interface Player {
  username: string;
  hand: Card[];
  score: number;
  bullet: number;
  mountain: number;
}

export interface GameState {
  gameId: string;
  players: Player[];
  phase: GamePhase;
  roundNumber: number;
  widow: Card[];
  dealerIndex: number;
  currentPlayerIndex: number;
  bids: Record<string, Contract>;
  declarer: string | null;
  contract: Contract | null;
  currentTrick: Record<string, Card>;
  completedTricks: Array<Record<string, Card>>;
  tricksWon: Record<string, number>;
  lastSeen: Record<string, number>;
  rules: GameRules;
}

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
  lastSeenSeconds: Record<string, number>;
  rules: GameRules;
}

// Message types for client-server communication
export type ClientMessage =
  | { type: 'register'; username: string; password: string }
  | { type: 'login'; username: string; password: string }
  | { type: 'createGame'; gameId: string; ruleId?: string }
  | { type: 'joinGame'; gameId: string }
  | { type: 'getState' }
  | { type: 'placeBid'; contract: Contract }
  | { type: 'exchangeWidow'; cards: Card[] }
  | { type: 'playCard'; card: Card }
  | { type: 'startNextRound' };

export type ServerMessage =
  | { type: 'authSuccess'; token: string; username: string }
  | { type: 'authError'; message: string }
  | { type: 'gameState'; state: PlayerView }
  | { type: 'error'; message: string }
  | { type: 'gameCreated'; gameId: string }
  | { type: 'gameJoined'; gameId: string };
