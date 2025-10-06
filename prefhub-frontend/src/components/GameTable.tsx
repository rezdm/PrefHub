import { useEffect } from 'react';
import { useGameStore } from '../store/gameStore';
import type { Card } from '../types';
import { cardToString, getSuitColor, getSuitSymbol, getRankSymbol } from '../types';
import './GameTable.css';

const GameTable = () => {
  const {
    gameState,
    username,
    leaveGame,
    refreshGameState,
    playCard,
    loading,
    error
  } = useGameStore();

  useEffect(() => {
    // Refresh game state every 2 seconds
    const interval = setInterval(() => {
      refreshGameState();
    }, 2000);

    return () => clearInterval(interval);
  }, [refreshGameState]);

  if (!gameState) {
    return (
      <div className="game-table-container">
        <div className="loading-game">Loading game...</div>
      </div>
    );
  }

  const handleCardClick = async (card: Card) => {
    if (!gameState.isYourTurn || loading) return;

    try {
      await playCard(card);
    } catch (err) {
      console.error('Failed to play card:', err);
    }
  };

  const handleLeaveGame = () => {
    if (window.confirm('Are you sure you want to leave the game?')) {
      leaveGame();
    }
  };

  const getPhaseDisplay = (): string => {
    const phaseMap: Record<string, string> = {
      WAITING_FOR_PLAYERS: 'Waiting for players',
      BIDDING: 'Bidding phase',
      WIDOW_EXCHANGE: 'Widow exchange',
      PLAYING: 'Playing',
      ROUND_COMPLETE: 'Round complete'
    };
    return phaseMap[gameState.phase] || gameState.phase;
  };

  const renderCard = (card: Card, isPlayable: boolean = false) => {
    const color = getSuitColor(card.suit);
    return (
      <div
        key={cardToString(card)}
        className={`card ${color} ${isPlayable ? 'playable' : ''} ${loading ? 'disabled' : ''}`}
        onClick={() => isPlayable && handleCardClick(card)}
      >
        <div className="card-rank">{getRankSymbol(card.rank)}</div>
        <div className="card-suit">{getSuitSymbol(card.suit)}</div>
      </div>
    );
  };

  return (
    <div className="game-table-container">
      <header className="game-header">
        <div className="game-info-bar">
          <div className="game-id">Game: {gameState.gameId}</div>
          <div className="phase-indicator">{getPhaseDisplay()}</div>
          <button className="leave-button" onClick={handleLeaveGame}>
            Leave Game
          </button>
        </div>
      </header>

      {error && (
        <div className="error-banner">
          {error}
        </div>
      )}

      <div className="game-table">
        {/* Player positions */}
        <div className="players-area">
          {/* West player */}
          {gameState.otherPlayers.length > 0 && (
            <div className="player-position west">
              <div className="player-card">
                <div className="player-name">{gameState.otherPlayers[0]}</div>
                <div className="player-status">
                  {gameState.currentPlayerUsername === gameState.otherPlayers[0] && (
                    <span className="turn-indicator">⏱ Turn</span>
                  )}
                </div>
                <div className="player-stats">
                  <span>Tricks: {gameState.tricksWon[gameState.otherPlayers[0]] || 0}</span>
                </div>
              </div>
            </div>
          )}

          {/* East player */}
          {gameState.otherPlayers.length > 1 && (
            <div className="player-position east">
              <div className="player-card">
                <div className="player-name">{gameState.otherPlayers[1]}</div>
                <div className="player-status">
                  {gameState.currentPlayerUsername === gameState.otherPlayers[1] && (
                    <span className="turn-indicator">⏱ Turn</span>
                  )}
                </div>
                <div className="player-stats">
                  <span>Tricks: {gameState.tricksWon[gameState.otherPlayers[1]] || 0}</span>
                </div>
              </div>
            </div>
          )}

          {/* Trick area (center) */}
          <div className="trick-area">
            {Object.keys(gameState.currentTrick).length > 0 ? (
              <>
                <div className="trick-label">Current Trick</div>
                <div className="trick-cards">
                  {Object.entries(gameState.currentTrick).map(([player, card]) => (
                    <div key={player} className="trick-card-wrapper">
                      {renderCard(card)}
                      <div className="trick-player-label">{player}</div>
                    </div>
                  ))}
                </div>
              </>
            ) : (
              <div className="trick-placeholder">
                {gameState.phase === 'PLAYING' ? 'Waiting for cards...' : 'No trick in progress'}
              </div>
            )}
          </div>

          {/* South player (current user) */}
          <div className="player-position south">
            <div className="player-card current-player">
              <div className="player-name">{username} (You)</div>
              <div className="player-status">
                {gameState.isYourTurn && (
                  <span className="turn-indicator active">⏱ Your Turn</span>
                )}
              </div>
              <div className="player-stats">
                <span>Tricks: {gameState.tricksWon[username || ''] || 0}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Player's hand */}
        <div className="hand-area">
          <div className="hand-label">Your Hand</div>
          <div className="hand-cards">
            {gameState.hand.length > 0 ? (
              gameState.hand.map((card) =>
                renderCard(card, gameState.isYourTurn && gameState.phase === 'PLAYING')
              )
            ) : (
              <div className="empty-hand">No cards</div>
            )}
          </div>
        </div>

        {/* Game info sidebar */}
        <div className="game-sidebar">
          <div className="sidebar-section">
            <h3>Game Info</h3>
            <p><strong>Round:</strong> {gameState.roundNumber}</p>
            {gameState.trumpSuit && (
              <p><strong>Trump:</strong> {getSuitSymbol(gameState.trumpSuit)}</p>
            )}
            {gameState.contract && (
              <p><strong>Contract:</strong> {gameState.contract}</p>
            )}
            {gameState.declarerUsername && (
              <p><strong>Declarer:</strong> {gameState.declarerUsername}</p>
            )}
          </div>

          <div className="sidebar-section">
            <h3>Scores</h3>
            {Object.entries(gameState.scores).map(([player, score]) => (
              <div key={player} className="score-line">
                <span>{player}:</span>
                <span>{score}</span>
              </div>
            ))}
          </div>

          <div className="sidebar-section">
            <h3>Next Action</h3>
            <p className="action-description">
              {gameState.nextActionDescription || 'Waiting...'}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default GameTable;
