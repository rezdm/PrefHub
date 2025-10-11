import { formatPresence, isPlayerOnline } from '../types';

interface PlayerCardProps {
  username: string;
  isCurrentUser?: boolean;
  isCurrentTurn: boolean;
  tricksWon: number;
  lastSeenSeconds?: number;
}

const PlayerCard = ({ username, isCurrentUser, isCurrentTurn, tricksWon, lastSeenSeconds }: PlayerCardProps) => {
  return (
    <div className={`player-card ${isCurrentUser ? 'current-player' : ''}`}>
      <div className="player-name">
        {username} {isCurrentUser && '(You)'}
        {lastSeenSeconds !== undefined && (
          <span className={`presence-indicator ${isPlayerOnline(lastSeenSeconds) ? 'online' : 'offline'}`}>
            {formatPresence(lastSeenSeconds)}
          </span>
        )}
      </div>
      <div className="player-status">
        {isCurrentTurn && (
          <span className={`turn-indicator ${isCurrentUser ? 'active' : ''}`}>⏱ {isCurrentUser ? 'Your Turn' : 'Turn'}</span>
        )}
      </div>
      <div className="player-stats">
        <span>Tricks: {tricksWon}</span>
      </div>
    </div>
  );
};

export default PlayerCard;
