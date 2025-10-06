package com.prefhub.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Player-specific view of the game state.
 * Shows only information that this player should see.
 */
public record PlayerView(
    String gameId,
    String playerUsername,
    GamePhase phase,
    int roundNumber,
    List<Card> hand,
    List<String> otherPlayers,
    String currentPlayerUsername,
    boolean isYourTurn,
    List<String> allowedActions,
    String nextActionDescription,
    Map<String, String> bids,
    Contract highestBid,
    List<Card> widow,
    Contract contract,
    String declarerUsername,
    Suit trumpSuit,
    Map<String, Card> currentTrick,
    Map<String, Integer> tricksWon,
    Map<String, Integer> scores,
    Map<String, Integer> bullets,
    Map<String, Integer> mountains,
    Map<String, Long> lastSeenSeconds, // seconds since last ping for each player
    GameRules rules
) {
    public PlayerView {
        hand = hand != null ? new ArrayList<>(hand) : new ArrayList<>();
        otherPlayers = otherPlayers != null ? new ArrayList<>(otherPlayers) : new ArrayList<>();
        allowedActions = allowedActions != null ? new ArrayList<>(allowedActions) : new ArrayList<>();
        widow = widow != null ? new ArrayList<>(widow) : new ArrayList<>();
    }

    public List<Card> getHand() { return new ArrayList<>(hand); }
    public List<String> getOtherPlayers() { return new ArrayList<>(otherPlayers); }
    public List<String> getAllowedActions() { return new ArrayList<>(allowedActions); }
    public List<Card> getWidow() { return new ArrayList<>(widow); }
}
