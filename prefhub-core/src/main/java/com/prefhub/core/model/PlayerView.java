package com.prefhub.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Player-specific view of the game state.
 * Shows only information that this player should see.
 */
public class PlayerView {
    private final String gameId;
    private final String playerUsername;
    private final GamePhase phase;
    private final int roundNumber;

    // Player's hand
    private final List<Card> hand;

    // Other players (without their cards)
    private final List<String> otherPlayers;

    // Current player
    private final String currentPlayerUsername;
    private final boolean isYourTurn;

    // Game state info
    private final List<String> allowedActions;
    private final String nextActionDescription;

    // Bidding phase
    private final Map<String, String> bids; // username -> contract name
    private final Contract highestBid;

    // Widow (only visible to declarer during exchange)
    private final List<Card> widow;

    // Playing phase
    private final Contract contract;
    private final String declarerUsername;
    private final Suit trumpSuit;
    private final Map<String, Card> currentTrick; // username -> card
    private final Map<String, Integer> tricksWon;

    // Scores
    private final Map<String, Integer> scores;
    private final Map<String, Integer> bullets;
    private final Map<String, Integer> mountains;

    public PlayerView(String gameId, String playerUsername, GamePhase phase, int roundNumber,
                     List<Card> hand, List<String> otherPlayers, String currentPlayerUsername,
                     boolean isYourTurn, List<String> allowedActions, String nextActionDescription,
                     Map<String, String> bids, Contract highestBid, List<Card> widow,
                     Contract contract, String declarerUsername, Suit trumpSuit,
                     Map<String, Card> currentTrick, Map<String, Integer> tricksWon,
                     Map<String, Integer> scores, Map<String, Integer> bullets, Map<String, Integer> mountains) {
        this.gameId = gameId;
        this.playerUsername = playerUsername;
        this.phase = phase;
        this.roundNumber = roundNumber;
        this.hand = hand != null ? new ArrayList<>(hand) : new ArrayList<>();
        this.otherPlayers = otherPlayers != null ? new ArrayList<>(otherPlayers) : new ArrayList<>();
        this.currentPlayerUsername = currentPlayerUsername;
        this.isYourTurn = isYourTurn;
        this.allowedActions = allowedActions != null ? new ArrayList<>(allowedActions) : new ArrayList<>();
        this.nextActionDescription = nextActionDescription;
        this.bids = bids;
        this.highestBid = highestBid;
        this.widow = widow != null ? new ArrayList<>(widow) : new ArrayList<>();
        this.contract = contract;
        this.declarerUsername = declarerUsername;
        this.trumpSuit = trumpSuit;
        this.currentTrick = currentTrick;
        this.tricksWon = tricksWon;
        this.scores = scores;
        this.bullets = bullets;
        this.mountains = mountains;
    }

    // Getters
    public String getGameId() { return gameId; }
    public String getPlayerUsername() { return playerUsername; }
    public GamePhase getPhase() { return phase; }
    public int getRoundNumber() { return roundNumber; }
    public List<Card> getHand() { return new ArrayList<>(hand); }
    public List<String> getOtherPlayers() { return new ArrayList<>(otherPlayers); }
    public String getCurrentPlayerUsername() { return currentPlayerUsername; }
    public boolean isYourTurn() { return isYourTurn; }
    public List<String> getAllowedActions() { return new ArrayList<>(allowedActions); }
    public String getNextActionDescription() { return nextActionDescription; }
    public Map<String, String> getBids() { return bids; }
    public Contract getHighestBid() { return highestBid; }
    public List<Card> getWidow() { return new ArrayList<>(widow); }
    public Contract getContract() { return contract; }
    public String getDeclarerUsername() { return declarerUsername; }
    public Suit getTrumpSuit() { return trumpSuit; }
    public Map<String, Card> getCurrentTrick() { return currentTrick; }
    public Map<String, Integer> getTricksWon() { return tricksWon; }
    public Map<String, Integer> getScores() { return scores; }
    public Map<String, Integer> getBullets() { return bullets; }
    public Map<String, Integer> getMountains() { return mountains; }
}
