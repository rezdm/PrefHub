import { useState } from 'react';
import type { Card } from '../types';
import { sortCards, cardToString } from '../types';
import PlayingCard from './PlayingCard';

interface WidowExchangePanelProps {
  widow: Card[];
  hand: Card[];
  onExchange: (cards: Card[]) => void;
  loading: boolean;
}

const WidowExchangePanel = ({ widow, hand, onExchange, loading }: WidowExchangePanelProps) => {
  const [selectedCards, setSelectedCards] = useState<Card[]>([]);

  const handleCardClick = (card: Card) => {
    setSelectedCards(prev => {
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

  const handleExchange = () => {
    if (selectedCards.length === 2) {
      onExchange(selectedCards);
      setSelectedCards([]);
    }
  };

  return (
    <div className="action-panel widow-panel">
      <h3>Exchange Widow - Select 2 cards to discard</h3>
      <div className="widow-cards">
        <div className="widow-label">Widow Cards:</div>
        <div className="widow-display">
          {widow.map(card => (
            <PlayingCard key={cardToString(card)} card={card} />
          ))}
        </div>
      </div>
      <div className="hand-selection">
        <div className="hand-label">Your Hand (select 2 to discard):</div>
        <div className="hand-cards-selection">
          {sortCards(hand).map(card => (
            <PlayingCard
              key={cardToString(card)}
              card={card}
              isSelectable={true}
              isSelected={selectedCards.some(c => cardToString(c) === cardToString(card))}
              onClick={handleCardClick}
            />
          ))}
        </div>
      </div>
      <button
        className="primary-button exchange-button"
        onClick={handleExchange}
        disabled={selectedCards.length !== 2 || loading}
      >
        Exchange Selected Cards ({selectedCards.length}/2)
      </button>
    </div>
  );
};

export default WidowExchangePanel;
