import { useEffect } from 'react';
import { useGameStore } from '../store/gameStore';
import type { Card, Contract } from '../types';
import { getSuitSymbol, sortCards } from '../types';
import PlayerCard from './PlayerCard';
import PlayingCard from './PlayingCard';
import BiddingPanel from './BiddingPanel';
import WidowExchangePanel from './WidowExchangePanel';
import RoundCompletePanel from './RoundCompletePanel';
import './GameTable.css';

const GameTable = () => {
  const {
    gameState,
    username,
    leaveGame,
    refreshGameState,
    playCard,
    placeBid,
    exchangeWidow,
    startNextRound,
    loading,
    error
  } = useGameStore();

  // No more polling! WebSocket provides real-time updates

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

  const handleBid = async (contract: Contract) => {
    if (loading) return;
    try {
      await placeBid(contract);
    } catch (err) {
      console.error('Failed to place bid:', err);
    }
  };

  const handleExchangeWidow = async (cardsToDiscard: Card[]) => {
    if (loading) return;
    try {
      await exchangeWidow(cardsToDiscard);
    } catch (err) {
      console.error('Failed to exchange widow:', err);
    }
  };

  const handleNextRound = async () => {
    if (loading) return;
    try {
      await startNextRound();
    } catch (err) {
      console.error('Failed to start next round:', err);
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

      {/* Bidding Phase UI */}
      {gameState.phase === 'BIDDING' && gameState.isYourTurn && (
        <BiddingPanel onBid={handleBid} loading={loading} />
      )}

      {/* Widow Exchange UI */}
      {gameState.phase === 'WIDOW_EXCHANGE' && gameState.isYourTurn && gameState.widow.length > 0 && (
        <WidowExchangePanel
          widow={gameState.widow}
          hand={gameState.hand}
          onExchange={handleExchangeWidow}
          loading={loading}
        />
      )}

      {/* Round Complete UI */}
      {gameState.phase === 'ROUND_COMPLETE' && (
        <RoundCompletePanel
          tricksWon={gameState.tricksWon}
          isYourTurn={gameState.isYourTurn}
          onNextRound={handleNextRound}
          loading={loading}
        />
      )}

      <div className="game-table">
        {/* Player positions */}
        <div className="players-area">
          {/* West player (left) */}
          {gameState.otherPlayers.length > 0 && (
            <div className="player-position west">
              <PlayerCard
                username={gameState.otherPlayers[0]}
                isCurrentTurn={gameState.currentPlayerUsername === gameState.otherPlayers[0]}
                tricksWon={gameState.tricksWon[gameState.otherPlayers[0]] || 0}
                lastSeenSeconds={gameState.lastSeenSeconds[gameState.otherPlayers[0]]}
              />
            </div>
          )}

          {/* East player (right) */}
          {gameState.otherPlayers.length > 1 && (
            <div className="player-position east">
              <PlayerCard
                username={gameState.otherPlayers[1]}
                isCurrentTurn={gameState.currentPlayerUsername === gameState.otherPlayers[1]}
                tricksWon={gameState.tricksWon[gameState.otherPlayers[1]] || 0}
                lastSeenSeconds={gameState.lastSeenSeconds[gameState.otherPlayers[1]]}
              />
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
                      <PlayingCard card={card} />
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

          {/* South player (current user - bottom) */}
          <div className="player-position south">
            <PlayerCard
              username={username || ''}
              isCurrentUser={true}
              isCurrentTurn={gameState.isYourTurn}
              tricksWon={gameState.tricksWon[username || ''] || 0}
              lastSeenSeconds={gameState.lastSeenSeconds[username || '']}
            />
          </div>
        </div>

        {/* Player's hand */}
        <div className="hand-area">
          <div className="hand-label">Your Hand</div>
          <div className="hand-cards">
            {gameState.hand.length > 0 ? (
              sortCards(gameState.hand).map((card) => (
                <PlayingCard
                  key={`${card.suit}-${card.rank}`}
                  card={card}
                  isPlayable={gameState.isYourTurn && gameState.phase === 'PLAYING'}
                  onClick={handleCardClick}
                  disabled={loading}
                />
              ))
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
