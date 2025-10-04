package com.prefhub.core.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serial;
import java.util.*;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.ANY,
                setterVisibility = JsonAutoDetect.Visibility.ANY)
public class GameState implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String gameId;
    private List<Player> players;
    private List<Card> widow; // прикуп (2 карты)
    private GamePhase phase;
    private int dealerIndex;
    private int currentPlayerIndex;
    private GameRules rules; // правила игры

    // Торговля
    private Map<Player, Contract> bids;
    private Player declarer; // игрок взявший взятку
    private Contract contract;

    // Разыгрывание
    private Trick currentTrick;
    private List<Trick> completedTricks;
    private Map<Player, Integer> tricksWon;

    private int roundNumber;

    // Default constructor for Jackson
    public GameState() {
        this.gameId = "";
        this.players = new ArrayList<>(3);
        this.widow = new ArrayList<>(2);
        this.phase = GamePhase.WAITING_FOR_PLAYERS;
        this.bids = new HashMap<>();
        this.completedTricks = new ArrayList<>();
        this.tricksWon = new HashMap<>();
        this.dealerIndex = 0;
        this.currentPlayerIndex = 0;
        this.roundNumber = 1;
        this.rules = new GameRules();
    }

    public GameState(String gameId) {
        this(gameId, null);
    }

    public GameState(String gameId, GameRules rules) {
        this.gameId = gameId;
        this.players = new ArrayList<>(3);
        this.widow = new ArrayList<>(2);
        this.phase = GamePhase.WAITING_FOR_PLAYERS;
        this.bids = new HashMap<>();
        this.completedTricks = new ArrayList<>();
        this.tricksWon = new HashMap<>();
        this.dealerIndex = 0;
        this.currentPlayerIndex = 0;
        this.roundNumber = 1;
        this.rules = rules != null ? rules : new GameRules(); // default rules if null
    }

    public String getGameId() {
        return gameId;
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public void addPlayer(Player player) {
        if (players.size() >= 3) {
            throw new IllegalStateException("Game already has 3 players");
        }
        players.add(player);
        tricksWon.put(player, 0);
    }

    public boolean isFull() {
        return players.size() == 3;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public int getDealerIndex() {
        return dealerIndex;
    }

    public void setDealerIndex(int dealerIndex) {
        this.dealerIndex = dealerIndex;
    }

    @JsonIgnore
    public Player getDealer() {
        if (players.isEmpty()) {
            return null;
        }
        return players.get(dealerIndex);
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    @JsonIgnore
    public Player getCurrentPlayer() {
        if (players.isEmpty()) {
            return null;
        }
        return players.get(currentPlayerIndex);
    }

    public void nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public List<Card> getWidow() {
        return new ArrayList<>(widow);
    }

    public void setWidow(List<Card> cards) {
        this.widow.clear();
        this.widow.addAll(cards);
    }

    public Map<Player, Contract> getBids() {
        return new HashMap<>(bids);
    }

    public void placeBid(Player player, Contract contract) {
        bids.put(player, contract);
    }

    public Player getDeclarer() {
        return declarer;
    }

    public void setDeclarer(Player declarer) {
        this.declarer = declarer;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public Trick getCurrentTrick() {
        return currentTrick;
    }

    public void setCurrentTrick(Trick trick) {
        this.currentTrick = trick;
    }

    public List<Trick> getCompletedTricks() {
        return new ArrayList<>(completedTricks);
    }

    public void addCompletedTrick(Trick trick) {
        completedTricks.add(trick);
    }

    public Map<Player, Integer> getTricksWon() {
        return new HashMap<>(tricksWon);
    }

    public void incrementTricksWon(Player player) {
        tricksWon.put(player, tricksWon.getOrDefault(player, 0) + 1);
    }

    public void resetTricksWon() {
        for (Player player : players) {
            tricksWon.put(player, 0);
        }
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public GameRules getRules() {
        return rules;
    }

    public void setRules(GameRules rules) {
        this.rules = rules;
    }

    public void nextRound() {
        roundNumber++;
        dealerIndex = (dealerIndex + 1) % players.size();
        bids.clear();
        declarer = null;
        contract = null;
        completedTricks.clear();
        resetTricksWon();
        widow.clear();
        currentTrick = null;
    }
}
