import { useEffect, useState } from 'react';
import { useGameStore } from '../store/gameStore';
import type { Card, Contract } from '../types';
import { cardToString, getSuitColor, getSuitSymbol, getRankSymbol, sortCards, formatPresence, isPlayerOnline } from '../types';
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

  const [selectedWidowCards, setSelectedWidowCards] = useState<Card[]>([]);

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

  const handleBid = async (contract: Contract) => {
    if (loading) return;
    try {
      await placeBid(contract);
    } catch (err) {
      console.error('Failed to place bid:', err);
    }
  };

  const handleWidowCardClick = (card: Card) => {
    setSelectedWidowCards(prev => {
      const isSelected = prev.some(c => cardToString(c) === cardToString(card));
      if (isSelected) {
        return prev.filter(c => cardToString(c) !== cardToString(card));
      } else {
        if (prev.length < 2) {
          return [...prev, card];
        }
        return prev;
      }
    });
  };

  const handleExchangeWidow = async () => {
    if (selectedWidowCards.length !== 2 || loading) return;
    try {
      await exchangeWidow(selectedWidowCards);
      setSelectedWidowCards([]);
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

  const renderCard = (card: Card, isPlayable: boolean = false) => {
    const color = getSuitColor(card.suit);
    return (
      <div
        key={cardToString(card)}
        className={`card ${color} ${isPlayable ? 'playable' : ''} ${loading ? 'disabled' : ''}`}
        data-suit-group={card.suit}
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

      {/* Bidding Phase UI */}
      {gameState.phase === 'BIDDING' && gameState.isYourTurn && (
        <div className="action-panel bidding-panel">
          <h3>Place Your Bid</h3>
          <div className="bidding-options">
            <button
              className="bid-button pass-bid"
              onClick={() => handleBid('PASS')}
              disabled={loading}
            >
              Pass
            </button>
            {(['SPADES', 'CLUBS', 'DIAMONDS', 'HEARTS'] as const).map(suit => (
              <div key={suit} className="suit-bids">
                <div className="suit-label">{getSuitSymbol(suit)}</div>
                {[6, 7, 8, 9, 10].map(level => {
                  const contract = `${['SIX', 'SEVEN', 'EIGHT', 'NINE', 'TEN'][level - 6]}_${suit}` as Contract;
                  return (
                    <button
                      key={contract}
                      className="bid-button"
                      onClick={() => handleBid(contract)}
                      disabled={loading}
                    >
                      {level}
                    </button>
                  );
                })}
              </div>
            ))}
            <button
              className="bid-button miser-bid"
              onClick={() => handleBid('MISER')}
              disabled={loading}
            >
              Miser
            </button>
          </div>
        </div>
      )}

      {/* Widow Exchange UI */}
      {gameState.phase === 'WIDOW_EXCHANGE' && gameState.isYourTurn && gameState.widow.length > 0 && (
        <div className="action-panel widow-panel">
          <h3>Exchange Widow - Select 2 cards to discard</h3>
          <div className="widow-cards">
            <div className="widow-label">Widow Cards:</div>
            <div className="widow-display">
              {gameState.widow.map(card => renderCard(card))}
            </div>
          </div>
          <div className="hand-selection">
            <div className="hand-label">Your Hand (select 2 to discard):</div>
            <div className="hand-cards-selection">
              {sortCards(gameState.hand).map(card => {
                const isSelected = selectedWidowCards.some(c => cardToString(c) === cardToString(card));
                return (
                  <div
                    key={cardToString(card)}
                    className={`card ${getSuitColor(card.suit)} ${isSelected ? 'selected' : ''} selectable`}
                    data-suit-group={card.suit}
                    onClick={() => handleWidowCardClick(card)}
                  >
                    <div className="card-rank">{getRankSymbol(card.rank)}</div>
                    <div className="card-suit">{getSuitSymbol(card.suit)}</div>
                  </div>
                );
              })}
            </div>
          </div>
          <button
            className="primary-button exchange-button"
            onClick={handleExchangeWidow}
            disabled={selectedWidowCards.length !== 2 || loading}
          >
            Exchange Selected Cards ({selectedWidowCards.length}/2)
          </button>
        </div>
      )}

      {/* Round Complete UI */}
      {gameState.phase === 'ROUND_COMPLETE' && (
        <div className="action-panel round-complete-panel">
          <h3>Round Complete!</h3>
          <div className="round-summary">
            <h4>Tricks Won:</h4>
            {Object.entries(gameState.tricksWon).map(([player, tricks]) => (
              <div key={player} className="tricks-summary">
                <span>{player}:</span>
                <span>{tricks}</span>
              </div>
            ))}
          </div>
          {gameState.isYourTurn && (
            <button
              className="primary-button next-round-button"
              onClick={handleNextRound}
              disabled={loading}
            >
              Start Next Round
            </button>
          )}
        </div>
      )}

      <div className="game-table">
        {/* Player positions */}
        <div className="players-area">
          {/* West player */}
          {gameState.otherPlayers.length > 0 && (
            <div className="player-position west">
              <div className="player-card">
                <div className="player-name">
                  {gameState.otherPlayers[0]}
                  {gameState.lastSeenSeconds[gameState.otherPlayers[0]] !== undefined && (
                    <span className={`presence-indicator ${isPlayerOnline(gameState.lastSeenSeconds[gameState.otherPlayers[0]]) ? 'online' : 'offline'}`}>
                      {formatPresence(gameState.lastSeenSeconds[gameState.otherPlayers[0]])}
                    </span>
                  )}
                </div>
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
                <div className="player-name">
                  {gameState.otherPlayers[1]}
                  {gameState.lastSeenSeconds[gameState.otherPlayers[1]] !== undefined && (
                    <span className={`presence-indicator ${isPlayerOnline(gameState.lastSeenSeconds[gameState.otherPlayers[1]]) ? 'online' : 'offline'}`}>
                      {formatPresence(gameState.lastSeenSeconds[gameState.otherPlayers[1]])}
                    </span>
                  )}
                </div>
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
              <div className="player-name">
                {username} (You)
                {gameState.lastSeenSeconds[username || ''] !== undefined && (
                  <span className="presence-indicator online">
                    {formatPresence(gameState.lastSeenSeconds[username || ''])}
                  </span>
                )}
              </div>
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
              sortCards(gameState.hand).map((card) =>
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
