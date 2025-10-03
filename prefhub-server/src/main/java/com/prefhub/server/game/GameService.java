package com.prefhub.server.game;

import com.prefhub.core.model.*;
import com.prefhub.server.persistence.GamePersistence;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameService {
    private final Map<String, GameState> activeGames = new ConcurrentHashMap<>();
    private final GamePersistence persistence;

    public GameService(GamePersistence persistence) {
        this.persistence = persistence;
    }

    public GameState createGame(String gameId) {
        if (activeGames.containsKey(gameId)) {
            throw new IllegalArgumentException("Game already exists: " + gameId);
        }
        GameState gameState = new GameState(gameId);
        activeGames.put(gameId, gameState);
        persistence.saveGame(gameState);
        return gameState;
    }

    public GameState joinGame(String gameId, String username) {
        GameState gameState = activeGames.get(gameId);
        if (gameState == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        if (gameState.isFull()) {
            throw new IllegalStateException("Game is full");
        }

        Player player = new Player(username);
        gameState.addPlayer(player);

        if (gameState.isFull()) {
            startRound(gameState);
        }

        persistence.saveGame(gameState);
        return gameState;
    }

    public GameState getGame(String gameId) {
        return activeGames.get(gameId);
    }

    public List<GameState> getAllGames() {
        return new ArrayList<>(activeGames.values());
    }

    public PlayerView getPlayerView(String gameId, String username) {
        GameState gameState = getGame(gameId);
        if (gameState == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }

        Player player = findPlayer(gameState, username);
        Player currentPlayer = gameState.getPlayers().size() > 0 ? gameState.getCurrentPlayer() : null;
        boolean isYourTurn = currentPlayer != null && currentPlayer.equals(player);

        // Determine allowed actions and next step description
        List<String> allowedActions = new ArrayList<>();
        String nextActionDescription;

        switch (gameState.getPhase()) {
            case WAITING_FOR_PLAYERS:
                nextActionDescription = "Ожидание игроков (" + gameState.getPlayers().size() + "/3)";
                break;

            case BIDDING:
                if (isYourTurn) {
                    allowedActions.add("BID");
                    nextActionDescription = "Ваш ход! Сделайте заявку (или пас)";
                } else {
                    nextActionDescription = "Ждем заявку от " + currentPlayer.getUsername();
                }
                break;

            case WIDOW_EXCHANGE:
                if (player.equals(gameState.getDeclarer())) {
                    allowedActions.add("EXCHANGE_WIDOW");
                    nextActionDescription = "Вы взяли игру! Возьмите прикуп и сбросьте 2 карты";
                } else {
                    nextActionDescription = "Ждем, пока " + gameState.getDeclarer().getUsername() + " обменяет прикуп";
                }
                break;

            case PLAYING:
                if (isYourTurn) {
                    allowedActions.add("PLAY_CARD");
                    nextActionDescription = "Ваш ход! Сыграйте карту";
                } else {
                    nextActionDescription = "Ждем хода от " + currentPlayer.getUsername();
                }
                break;

            case ROUND_COMPLETE:
                allowedActions.add("VIEW_SCORES");
                nextActionDescription = "Раунд завершен! Просмотрите результаты";
                break;

            case GAME_COMPLETE:
                nextActionDescription = "Игра завершена!";
                break;

            default:
                nextActionDescription = "Неизвестное состояние игры";
        }

        // Build bids map
        Map<String, String> bidsMap = new HashMap<>();
        Contract highestBid = null;
        for (Map.Entry<Player, Contract> entry : gameState.getBids().entrySet()) {
            bidsMap.put(entry.getKey().getUsername(), entry.getValue().getDisplayName());
            if (highestBid == null || entry.getValue().isHigherThan(highestBid)) {
                if (!entry.getValue().isPass()) {
                    highestBid = entry.getValue();
                }
            }
        }

        // Widow (only visible to declarer during exchange)
        List<Card> widow = null;
        if (gameState.getPhase() == GamePhase.WIDOW_EXCHANGE && player.equals(gameState.getDeclarer())) {
            widow = gameState.getWidow();
        }

        // Build current trick map
        Map<String, Card> currentTrickMap = new HashMap<>();
        if (gameState.getCurrentTrick() != null) {
            for (Map.Entry<Player, Card> entry : gameState.getCurrentTrick().getCardsPlayed().entrySet()) {
                currentTrickMap.put(entry.getKey().getUsername(), entry.getValue());
            }
        }

        // Build tricks won map
        Map<String, Integer> tricksWonMap = new HashMap<>();
        for (Map.Entry<Player, Integer> entry : gameState.getTricksWon().entrySet()) {
            tricksWonMap.put(entry.getKey().getUsername(), entry.getValue());
        }

        // Build scores, bullets, mountains maps
        Map<String, Integer> scoresMap = new HashMap<>();
        Map<String, Integer> bulletsMap = new HashMap<>();
        Map<String, Integer> mountainsMap = new HashMap<>();
        for (Player p : gameState.getPlayers()) {
            scoresMap.put(p.getUsername(), p.getScore());
            bulletsMap.put(p.getUsername(), p.getBullet());
            mountainsMap.put(p.getUsername(), p.getMountain());
        }

        // Other players
        List<String> otherPlayers = new ArrayList<>();
        for (Player p : gameState.getPlayers()) {
            if (!p.equals(player)) {
                otherPlayers.add(p.getUsername());
            }
        }

        return new PlayerView(
            gameState.getGameId(),
            username,
            gameState.getPhase(),
            gameState.getRoundNumber(),
            player.getHand(),
            otherPlayers,
            currentPlayer != null ? currentPlayer.getUsername() : null,
            isYourTurn,
            allowedActions,
            nextActionDescription,
            bidsMap,
            highestBid,
            widow,
            gameState.getContract(),
            gameState.getDeclarer() != null ? gameState.getDeclarer().getUsername() : null,
            gameState.getContract() != null ? gameState.getContract().getTrumpSuit() : null,
            currentTrickMap,
            tricksWonMap,
            scoresMap,
            bulletsMap,
            mountainsMap
        );
    }

    private void startRound(GameState gameState) {
        // Раздача карт
        Deck deck = new Deck();
        deck.shuffle();

        // Каждому игроку по 10 карт
        for (Player player : gameState.getPlayers()) {
            player.clearHand();
            player.addCards(deck.dealCards(10));
        }

        // 2 карты в прикуп
        gameState.setWidow(deck.dealCards(2));

        // Начинаем торговлю
        gameState.setPhase(GamePhase.BIDDING);
        gameState.setCurrentPlayerIndex((gameState.getDealerIndex() + 1) % 3);
    }

    public void placeBid(String gameId, String username, Contract contract) {
        GameState gameState = getGame(gameId);
        if (gameState == null) {
            throw new IllegalArgumentException("Game not found");
        }
        if (gameState.getPhase() != GamePhase.BIDDING) {
            throw new IllegalStateException("Not in bidding phase");
        }

        Player player = findPlayer(gameState, username);
        if (!player.equals(gameState.getCurrentPlayer())) {
            throw new IllegalStateException("Not your turn");
        }

        gameState.placeBid(player, contract);

        // Проверяем завершение торговли
        if (isBiddingComplete(gameState)) {
            finalizeBidding(gameState);
        } else {
            gameState.nextPlayer();
        }

        persistence.saveGame(gameState);
    }

    private boolean isBiddingComplete(GameState gameState) {
        Map<Player, Contract> bids = gameState.getBids();
        if (bids.size() < 3) {
            return false;
        }

        // Проверяем, есть ли хотя бы одна игровая заявка
        long gameBids = bids.values().stream()
                .filter(c -> !c.isPass())
                .count();

        if (gameBids == 0) {
            return true; // Все спасовали - распас
        }

        // Если есть один игрок с заявкой и два паса
        return gameBids == 1 && bids.values().stream().filter(Contract::isPass).count() == 2;
    }

    private void finalizeBidding(GameState gameState) {
        Map<Player, Contract> bids = gameState.getBids();

        // Находим максимальную заявку
        Player declarer = null;
        Contract maxContract = null;

        for (Map.Entry<Player, Contract> entry : bids.entrySet()) {
            if (!entry.getValue().isPass()) {
                if (maxContract == null || entry.getValue().isHigherThan(maxContract)) {
                    maxContract = entry.getValue();
                    declarer = entry.getKey();
                }
            }
        }

        if (declarer == null) {
            // Распас - начинаем новый раунд
            gameState.nextRound();
            startRound(gameState);
        } else {
            gameState.setDeclarer(declarer);
            gameState.setContract(maxContract);
            gameState.setPhase(GamePhase.WIDOW_EXCHANGE);
            gameState.setCurrentPlayerIndex(gameState.getPlayers().indexOf(declarer));
        }
    }

    public void exchangeWidow(String gameId, String username, List<Card> cardsToDiscard) {
        GameState gameState = getGame(gameId);
        if (gameState == null) {
            throw new IllegalArgumentException("Game not found");
        }
        if (gameState.getPhase() != GamePhase.WIDOW_EXCHANGE) {
            throw new IllegalStateException("Not in widow exchange phase");
        }

        Player declarer = gameState.getDeclarer();
        if (!declarer.getUsername().equals(username)) {
            throw new IllegalStateException("Only declarer can exchange widow");
        }

        if (cardsToDiscard.size() != 2) {
            throw new IllegalArgumentException("Must discard exactly 2 cards");
        }

        // Берем прикуп
        List<Card> widow = gameState.getWidow();
        declarer.addCards(widow);

        // Сбрасываем 2 карты
        for (Card card : cardsToDiscard) {
            declarer.removeCard(card);
        }

        // Начинаем розыгрыш
        gameState.setPhase(GamePhase.PLAYING);
        gameState.setCurrentPlayerIndex(gameState.getPlayers().indexOf(declarer));
        gameState.setCurrentTrick(new Trick());

        persistence.saveGame(gameState);
    }

    public void playCard(String gameId, String username, Card card) {
        GameState gameState = getGame(gameId);
        if (gameState == null) {
            throw new IllegalArgumentException("Game not found");
        }
        if (gameState.getPhase() != GamePhase.PLAYING) {
            throw new IllegalStateException("Not in playing phase");
        }

        Player player = findPlayer(gameState, username);
        if (!player.equals(gameState.getCurrentPlayer())) {
            throw new IllegalStateException("Not your turn");
        }

        // Validate card play (simplified - should check suit following rules)
        player.removeCard(card);
        gameState.getCurrentTrick().playCard(player, card);

        // Check if trick is complete
        if (gameState.getCurrentTrick().isComplete(3)) {
            Player winner = gameState.getCurrentTrick().getWinner(
                    gameState.getContract().getTrumpSuit());
            gameState.incrementTricksWon(winner);
            gameState.addCompletedTrick(gameState.getCurrentTrick());

            // Check if round is complete
            if (gameState.getCompletedTricks().size() == 10) {
                calculateScores(gameState);
                gameState.setPhase(GamePhase.ROUND_COMPLETE);
            } else {
                gameState.setCurrentTrick(new Trick());
                gameState.setCurrentPlayerIndex(gameState.getPlayers().indexOf(winner));
            }
        } else {
            gameState.nextPlayer();
        }

        persistence.saveGame(gameState);
    }

    private void calculateScores(GameState gameState) {
        Player declarer = gameState.getDeclarer();
        int tricksNeeded = gameState.getContract().getTricks();
        int tricksTaken = gameState.getTricksWon().get(declarer);

        if (gameState.getContract().isMiser()) {
            // Мизер: не должен взять ни одной взятки
            if (tricksTaken == 0) {
                declarer.addScore(10);
            } else {
                declarer.addMountain(10);
            }
        } else {
            // Обычный контракт
            if (tricksTaken >= tricksNeeded) {
                declarer.addScore(tricksNeeded);
            } else {
                declarer.addMountain(tricksNeeded);
            }

            // Вистующие
            for (Player player : gameState.getPlayers()) {
                if (!player.equals(declarer)) {
                    int tricks = gameState.getTricksWon().get(player);
                    player.addScore(tricks);
                }
            }
        }
    }

    public void startNextRound(String gameId) {
        GameState gameState = getGame(gameId);
        if (gameState == null) {
            throw new IllegalArgumentException("Game not found");
        }
        if (gameState.getPhase() != GamePhase.ROUND_COMPLETE) {
            throw new IllegalStateException("Round is not complete");
        }

        gameState.nextRound();
        startRound(gameState);
        persistence.saveGame(gameState);
    }

    private Player findPlayer(GameState gameState, String username) {
        return gameState.getPlayers().stream()
                .filter(p -> p.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not in game: " + username));
    }
}
