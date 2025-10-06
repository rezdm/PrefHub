import { useEffect } from 'react';
import { useGameStore } from './store/gameStore';
import Login from './components/Login';
import Lobby from './components/Lobby';
import GameTable from './components/GameTable';
import './App.css';

function App() {
  const { isAuthenticated, currentGameId, initializeAuth } = useGameStore();

  useEffect(() => {
    console.log('[App] useEffect running - initializing auth');

    // Check for auto-login URL parameters
    const params = new URLSearchParams(window.location.search);
    const autoToken = params.get('autoToken');
    const autoUsername = params.get('autoUsername');
    const autoGameId = params.get('autoGameId');

    if (autoToken && autoUsername) {
      console.log('[App] Auto-login detected:', autoUsername);
      // Set localStorage directly for auto-login
      localStorage.setItem('auth_token', autoToken);
      localStorage.setItem('username', autoUsername);

      // Store autoGameId in sessionStorage for Lobby to pick up
      if (autoGameId) {
        console.log('[App] Auto-game detected:', autoGameId);
        sessionStorage.setItem('autoGameId', autoGameId);
      }

      // Clear URL parameters
      window.history.replaceState({}, document.title, window.location.pathname);
    }

    // Initialize auth state from localStorage on mount
    initializeAuth();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  console.log('[App] Rendering - isAuthenticated:', isAuthenticated, 'currentGameId:', currentGameId);

  // Routing logic based on state
  if (!isAuthenticated) {
    console.log('[App] Showing Login');
    return <Login />;
  }

  if (currentGameId) {
    console.log('[App] Showing GameTable');
    return <GameTable />;
  }

  console.log('[App] Showing Lobby');
  return <Lobby />;
}

export default App;
