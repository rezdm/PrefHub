interface RoundCompletePanelProps {
  tricksWon: Record<string, number>;
  isYourTurn: boolean;
  onNextRound: () => void;
  loading: boolean;
}

const RoundCompletePanel = ({ tricksWon, isYourTurn, onNextRound, loading }: RoundCompletePanelProps) => {
  return (
    <div className="action-panel round-complete-panel">
      <h3>Round Complete!</h3>
      <div className="round-summary">
        <h4>Tricks Won:</h4>
        {Object.entries(tricksWon).map(([player, tricks]) => (
          <div key={player} className="tricks-summary">
            <span>{player}:</span>
            <span>{tricks}</span>
          </div>
        ))}
      </div>
      {isYourTurn && (
        <button
          className="primary-button next-round-button"
          onClick={onNextRound}
          disabled={loading}
        >
          Start Next Round
        </button>
      )}
    </div>
  );
};

export default RoundCompletePanel;
