import { useState, useEffect } from 'react';
import { useGameStore } from '../store/gameStore';
import { apiClient } from '../api/client';
import type { GameListItem } from '../types';
import './Lobby.css';

const Lobby = () => {
  const { username, logout, createGame, joinGame, error, loading } = useGameStore();
  const [games, setGames] = useState<GameListItem[]>([]);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newGameId, setNewGameId] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const [loadingGames, setLoadingGames] = useState(false);

  const loadGames = async () => {
    try {
      setLoadingGames(true);
      const gameList = await apiClient.listGames();
      // Normalize the data - backend might return player objects instead of strings
      const normalized = gameList.map((game: any) => ({
        ...game,
        players: game.players.map((p: any) =>
          typeof p === 'string' ? p : p.username || p.playerUsername || String(p)
        )
      }));
      setGames(normalized);
    } catch (err) {
      console.error('Failed to load games:', err);
      setGames([]);
    } finally {
      setLoadingGames(false);
    }
  };

  useEffect(() => {
    loadGames();

    // Check if we should auto-load a game from sessionStorage
    const autoGameId = sessionStorage.getItem('autoGameId');

    if (autoGameId) {
      console.log('[Lobby] Auto-loading game:', autoGameId);
      // Clear sessionStorage immediately
      sessionStorage.removeItem('autoGameId');

      // Fetch game state after a short delay to ensure API is ready
      setTimeout(async () => {
        try {
          console.log('[Lobby] Fetching game state for:', autoGameId);
          // Player is already joined via backend, just fetch the state
          const gameState = await apiClient.getGameState(autoGameId);
          console.log('[Lobby] Game state fetched:', gameState);

          // Manually set the game state in the store
          useGameStore.setState({
            currentGameId: autoGameId,
            gameState: gameState,
          });
          console.log('[Lobby] Successfully loaded game');
        } catch (err) {
          console.error('[Lobby] Failed to load game:', err);
          // If fetching fails, try joining
          try {
            console.log('[Lobby] Attempting to join game as fallback');
            await joinGame(autoGameId);
          } catch (joinErr) {
            console.error('[Lobby] Failed to join game:', joinErr);
          }
        }
      }, 1000);
    } else {
      // Check if user has an active game (reconnection scenario)
      checkForActiveGame();
    }

    // Refresh game list every 5 seconds
    const interval = setInterval(loadGames, 5000);
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const checkForActiveGame = async () => {
    try {
      const response = await apiClient.getActiveGame();
      if (response.gameId) {
        console.log('[Lobby] Found active game:', response.gameId);
        // Ask user if they want to rejoin
        if (window.confirm(`You have an active game (${response.gameId}). Would you like to rejoin?`)) {
          await joinGame(response.gameId);
        }
      }
    } catch (err) {
      console.error('[Lobby] Failed to check for active game:', err);
    }
  };

  const handleCreateGame = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);

    if (!newGameId.trim()) {
      setLocalError('Game ID is required');
      return;
    }

    try {
      await createGame(newGameId, 'CLASSIC');
      setShowCreateModal(false);
      setNewGameId('');
    } catch (err) {
      // Error handled by store
    }
  };

  const handleJoinGame = async (gameId: string) => {
    try {
      await joinGame(gameId);
    } catch (err) {
      // Error handled by store
    }
  };

  const getPhaseDisplay = (phase: string): string => {
    const phaseMap: Record<string, string> = {
      WAITING_FOR_PLAYERS: 'Waiting for players',
      BIDDING: 'Bidding',
      WIDOW_EXCHANGE: 'Widow exchange',
      PLAYING: 'Playing',
      ROUND_COMPLETE: 'Round complete'
    };
    return phaseMap[phase] || phase;
  };

  return (
    <div className="lobby-container">
      <header className="lobby-header">
        <div className="header-content">
          <h1>PrefHub Lobby</h1>
          <div className="user-info">
            <span className="username-display">Welcome, {username}</span>
            <button onClick={logout} className="logout-button">
              Logout
            </button>
          </div>
        </div>
      </header>

      <main className="lobby-main">
        {(error || localError) && (
          <div className="error-banner">
            {error || localError}
          </div>
        )}

        <div className="lobby-actions">
          <button
            className="create-game-button"
            onClick={() => setShowCreateModal(true)}
            disabled={loading}
          >
            Create New Game
          </button>
          <button
            className="refresh-button"
            onClick={loadGames}
            disabled={loadingGames}
          >
            {loadingGames ? 'Refreshing...' : 'Refresh Games'}
          </button>
        </div>

        <div className="games-section">
          <h2>Available Games</h2>
          {loadingGames && games.length === 0 ? (
            <div className="loading-message">Loading games...</div>
          ) : games.length === 0 ? (
            <div className="no-games-message">
              <p>No games available. Create one to get started!</p>
            </div>
          ) : (
            <div className="games-grid">
              {games.map((game) => (
                <div key={game.gameId} className="game-card">
                  <h3>{game.gameId}</h3>
                  <div className="game-info">
                    <p>
                      <strong>Players:</strong> {game.players.length}/3
                    </p>
                    <p>
                      <strong>Status:</strong> {getPhaseDisplay(game.phase)}
                    </p>
                    <div className="player-list">
                      {game.players.map((player) => (
                        <span key={player} className="player-badge">
                          {player}
                        </span>
                      ))}
                    </div>
                  </div>
                  <button
                    className="join-button"
                    onClick={() => handleJoinGame(game.gameId)}
                    disabled={
                      loading ||
                      game.players.length >= 3 ||
                      game.phase !== 'WAITING_FOR_PLAYERS'
                    }
                  >
                    {game.players.length >= 3
                      ? 'Full'
                      : game.phase !== 'WAITING_FOR_PLAYERS'
                      ? 'In Progress'
                      : 'Join Game'}
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </main>

      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Create New Game</h2>
            <form onSubmit={handleCreateGame}>
              <div className="form-group">
                <label htmlFor="gameId">Game ID</label>
                <input
                  id="gameId"
                  type="text"
                  value={newGameId}
                  onChange={(e) => setNewGameId(e.target.value)}
                  placeholder="Enter game ID"
                  autoFocus
                  disabled={loading}
                />
              </div>
              <div className="modal-actions">
                <button
                  type="button"
                  className="cancel-button"
                  onClick={() => setShowCreateModal(false)}
                  disabled={loading}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="primary-button"
                  disabled={loading}
                >
                  {loading ? 'Creating...' : 'Create Game'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Lobby;
