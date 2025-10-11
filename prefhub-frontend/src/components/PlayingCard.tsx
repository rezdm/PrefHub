import type { Card } from '../types';
import { getSuitColor, getRankSymbol, getSuitSymbol, cardToString } from '../types';

interface PlayingCardProps {
  card: Card;
  isPlayable?: boolean;
  isSelectable?: boolean;
  isSelected?: boolean;
  onClick?: (card: Card) => void;
  disabled?: boolean;
}

const PlayingCard = ({
  card,
  isPlayable = false,
  isSelectable = false,
  isSelected = false,
  onClick,
  disabled = false
}: PlayingCardProps) => {
  const color = getSuitColor(card.suit);

  const handleClick = () => {
    if ((isPlayable || isSelectable) && onClick && !disabled) {
      onClick(card);
    }
  };

  const className = [
    'card',
    color,
    isPlayable && 'playable',
    isSelectable && 'selectable',
    isSelected && 'selected',
    disabled && 'disabled'
  ].filter(Boolean).join(' ');

  return (
    <div
      key={cardToString(card)}
      className={className}
      data-suit-group={card.suit}
      onClick={handleClick}
    >
      <div className="card-rank">{getRankSymbol(card.rank)}</div>
      <div className="card-suit">{getSuitSymbol(card.suit)}</div>
    </div>
  );
};

export default PlayingCard;
