package com.prefhub.core.model;

import java.util.HashMap;
import java.util.Map;

public class Trick implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<Player, Card> cardsPlayed;
    private Player leadPlayer;
    private Suit leadSuit;

    public Trick() {
        this.cardsPlayed = new HashMap<>();
    }

    public void playCard(Player player, Card card) {
        if (cardsPlayed.isEmpty()) {
            leadPlayer = player;
            leadSuit = card.getSuit();
        }
        cardsPlayed.put(player, card);
    }

    public Map<Player, Card> getCardsPlayed() {
        return new HashMap<>(cardsPlayed);
    }

    public Player getLeadPlayer() {
        return leadPlayer;
    }

    public Suit getLeadSuit() {
        return leadSuit;
    }

    public boolean isComplete(int playerCount) {
        return cardsPlayed.size() == playerCount;
    }

    public Player getWinner(Suit trumpSuit) {
        if (cardsPlayed.isEmpty()) {
            return null;
        }

        Player winner = null;
        Card winningCard = null;

        for (final var entry : cardsPlayed.entrySet()) {
            final var card = entry.getValue();

            if (winningCard == null) {
                winner = entry.getKey();
                winningCard = card;
                continue;
            }

            // Козырь бьет некозырную
            final var currentIsTrump = trumpSuit != null && card.getSuit() == trumpSuit;
            final var winningIsTrump = trumpSuit != null && winningCard.getSuit() == trumpSuit;

            if (currentIsTrump && !winningIsTrump) {
                winner = entry.getKey();
                winningCard = card;
            } else if (!currentIsTrump && winningIsTrump) {
                // winningCard остается
            } else if (card.getSuit() == winningCard.getSuit()) {
                // Одной масти - сравниваем старшинство
                if (card.getRank().getValue() > winningCard.getRank().getValue()) {
                    winner = entry.getKey();
                    winningCard = card;
                }
            }
            // Если карта не козырь и не той масти что ходили - она не бьет
        }

        return winner;
    }
}
