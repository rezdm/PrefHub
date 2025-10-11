import type { Contract } from '../types';
import { getSuitSymbol } from '../types';

interface BiddingPanelProps {
  onBid: (contract: Contract) => void;
  loading: boolean;
}

const BiddingPanel = ({ onBid, loading }: BiddingPanelProps) => {
  return (
    <div className="action-panel bidding-panel">
      <h3>Place Your Bid</h3>
      <div className="bidding-options">
        <button
          className="bid-button pass-bid"
          onClick={() => onBid('PASS')}
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
                  onClick={() => onBid(contract)}
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
          onClick={() => onBid('MISER')}
          disabled={loading}
        >
          Miser
        </button>
      </div>
    </div>
  );
};

export default BiddingPanel;
