package com.prefhub.server.game;

import com.prefhub.core.model.*;
import com.google.inject.Inject;
import com.prefhub.server.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameService {
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);
    private final Map<String, GameState> activeGames = new ConcurrentHashMap<>();
    private final GameRepository gameRepository;
    private final RulesLoader rulesLoader;

    @Inject
    public GameService(final GameRepository gameRepository, final RulesLoader rulesLoader) {
        this.gameRepository = gameRepository;
        this.rulesLoader = rulesLoader;
        loadExistingGames();
    }

    private void loadExistingGames() {
        final var games = gameRepository.findAll();
        for (final var game : games) {
            activeGames.put(game.getGameId(), game);
        }
        logger.info("Loaded {} games from storage", games.size());
    }

    public GameState createGame(final String gameId) {
        return createGame(gameId, null);
    }

    public GameState createGame(final String gameId, final String ruleId) {
        if (activeGames.containsKey(gameId)) {
            throw new IllegalArgumentException("Game already exists: " + gameId);
        }

        final GameRules rules;
        if (ruleId != null && !ruleId.isEmpty()) {
            rules = rulesLoader.getRules(ruleId);
        } else {
            rules = rulesLoader.getDefaultRules();
        }

        final var gameState = new GameState(gameId, rules);
        activeGames.put(gameId, gameState);
        gameRepository.save(gameState);
        return gameState;
    }

    public Map<String, String> getAvailableRules() {
        return rulesLoader.getAvailableRulesList();
    }

    public List<Contract> getAvailableBids(final String gameId) {
        final var gameState = getGame(gameId);
        if (gameState == null) {
            throw new IllegalArgumentException("Game not found");
        }
        return RulesValidator.getAvailableBids(gameState);
    }

    public GameState joinGame(final String gameId, final String username) {
        final var gameState = activeGames.get(gameId);
        if (gameState == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        if (gameState.isFull()) {
            throw new IllegalStateException("Game is full");
        }

        final var player = new Player(username);
        gameState.addPlayer(player);

        if (gameState.isFull()) {
            startRound(gameState);
        }

        gameRepository.save(gameState);
        return gameState;
    }

    public GameState getGame(final String gameId) {
        return activeGames.get(gameId);
    }

    public List<GameState> getAllGames() {
        return new ArrayList<>(activeGames.values());
    }

    public PlayerView getPlayerView(final String gameId, final String username) {
        final var gameState = getGame(gameId);
        if (gameState == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }

        final var player = findPlayer(gameState, username);
        final var currentPlayer = !gameState.getPlayers().isEmpty() ? gameState.getCurrentPlayer() : null;
        final var isYourTurn = currentPlayer != null && currentPlayer.equals(player);

        // Determine allowed actions and next step description
        final var allowedActions = new ArrayList<String>();
        final String nextActionDescription;

        switch (gameState.getPhase()) {
            case WAITING_FOR_PLAYERS:
                nextActionDescription = "Ожидание игроков (" + gameState.getPlayers().size() + "/3)";
                break;

            case BIDDING:
                if (isYourTurn) {
                    allowedActions.add("BID");
                    nextActionDescription = "Ваш ход! Сделайте заявку (или пас)";
                } else {
                    nextActionDescription = "Ждем заявку от " + (currentPlayer != null ? currentPlayer.getUsername() : null);
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
                    nextActionDescription = "Ждем хода от " + (currentPlayer != null ? currentPlayer.getUsername() : null);
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
        final var bidsMap = new HashMap<String, String>();
        Contract highestBid = null;
        for (final var entry : gameState.getBids().entrySet()) {
            bidsMap.put(entry.getKey().getUsername(), entry.getValue().getDisplayName());
            if (highestBid == null || entry.getValue().isHigherThan(highestBid)) {
                if (!entry.getValue().isPass()) {
                    highestBid = entry.getValue();
                }
            }
        }

        // Widow (only visible to declarer during exchange)
        final List<Card> widow;
        if (gameState.getPhase() == GamePhase.WIDOW_EXCHANGE && player.equals(gameState.getDeclarer())) {
            widow = gameState.getWidow();
        } else {
            widow = null;
        }

        // Build current trick map
        final var currentTrickMap = new HashMap<String, Card>();
        if (gameState.getCurrentTrick() != null) {
            for (final var entry : gameState.getCurrentTrick().getCardsPlayed().entrySet()) {
                currentTrickMap.put(entry.getKey().getUsername(), entry.getValue());
            }
        }

        // Build tricks won map
        final var tricksWonMap = new HashMap<String, Integer>();
        for (final var entry : gameState.getTricksWon().entrySet()) {
            tricksWonMap.put(entry.getKey().getUsername(), entry.getValue());
        }

        // Build scores, bullets, mountains maps
        final var scoresMap = new HashMap<String, Integer>();
        final var bulletsMap = new HashMap<String, Integer>();
        final var mountainsMap = new HashMap<String, Integer>();
        for (final var p : gameState.getPlayers()) {
            scoresMap.put(p.getUsername(), p.getScore());
            bulletsMap.put(p.getUsername(), p.getBullet());
            mountainsMap.put(p.getUsername(), p.getMountain());
        }

        // Other players
        final var otherPlayers = new ArrayList<String>();
        for (final var p : gameState.getPlayers()) {
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
            mountainsMap,
            gameState.getRules()
        );
    }

    private void startRound(final GameState gameState) {
        // Раздача карт
        final var deck = new Deck();
        deck.shuffle();

        // Каждому игроку по 10 карт
        for (final var player : gameState.getPlayers()) {
            player.clearHand();
            player.addCards(deck.dealCards(10));
        }

        // 2 карты в прикуп
        gameState.setWidow(deck.dealCards(2));

        // Начинаем торговлю
        gameState.setPhase(GamePhase.BIDDING);
        gameState.setCurrentPlayerIndex((gameState.getDealerIndex() + 1) % 3);
    }

    public void placeBid(final String gameId, final String username, final Contract contract) {
        final var gameState = getGame(gameId);
        if (gameState == null) {
            throw new IllegalArgumentException("Game not found");
        }
        if (gameState.getPhase() != GamePhase.BIDDING) {
            throw new IllegalStateException("Not in bidding phase");
        }

        final var player = findPlayer(gameState, username);
        if (!player.equals(gameState.getCurrentPlayer())) {
            throw new IllegalStateException("Not your turn");
        }

        // Validate bid according to rules
        RulesValidator.validateBid(gameState, contract);

        gameState.placeBid(player, contract);

        // Проверяем завершение торговли
        if (isBiddingComplete(gameState)) {
            finalizeBidding(gameState);
        } else {
            gameState.nextPlayer();
        }

        gameRepository.save(gameState);
    }

    private boolean isBiddingComplete(final GameState gameState) {
        final var bids = gameState.getBids();
        if (bids.size() < 3) {
            return false;
        }

        // Проверяем, есть ли хотя бы одна игровая заявка
        final var gameBids = bids.values().stream()
                .filter(c -> !c.isPass())
                .count();

        if (gameBids == 0) {
            return true; // Все спасовали - распас
        }

        // Если есть один игрок с заявкой и два паса
        return gameBids == 1 && bids.values().stream().filter(Contract::isPass).count() == 2;
    }

    private void finalizeBidding(final GameState gameState) {
        final var bids = gameState.getBids();

        // Находим максимальную заявку
        Player declarer = null;
        Contract maxContract = null;

        for (final var entry : bids.entrySet()) {
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

    public void exchangeWidow(final String gameId, final String username, final List<Card> cardsToDiscard) {
        final var gameState = getGame(gameId);
        if (gameState == null) {
            throw new IllegalArgumentException("Game not found");
        }
        if (gameState.getPhase() != GamePhase.WIDOW_EXCHANGE) {
            throw new IllegalStateException("Not in widow exchange phase");
        }

        final var declarer = gameState.getDeclarer();
        if (!declarer.getUsername().equals(username)) {
            throw new IllegalStateException("Only declarer can exchange widow");
        }

        if (cardsToDiscard.size() != 2) {
            throw new IllegalArgumentException("Must discard exactly 2 cards");
        }

        // Берем прикуп
        final var widow = gameState.getWidow();
        declarer.addCards(widow);

        // Сбрасываем 2 карты
        for (final var card : cardsToDiscard) {
            declarer.removeCard(card);
        }

        // Начинаем розыгрыш
        gameState.setPhase(GamePhase.PLAYING);
        gameState.setCurrentPlayerIndex(gameState.getPlayers().indexOf(declarer));
        gameState.setCurrentTrick(new Trick());

        gameRepository.save(gameState);
    }

    public void playCard(final String gameId, final String username, final Card card) {
        final var gameState = getGame(gameId);
        if (gameState == null) {
            throw new IllegalArgumentException("Game not found");
        }
        if (gameState.getPhase() != GamePhase.PLAYING) {
            throw new IllegalStateException("Not in playing phase");
        }

        final var player = findPlayer(gameState, username);
        if (!player.equals(gameState.getCurrentPlayer())) {
            throw new IllegalStateException("Not your turn");
        }

        // Validate card play (simplified - should check suit following rules)
        player.removeCard(card);
        gameState.getCurrentTrick().playCard(player, card);

        // Check if trick is complete
        if (gameState.getCurrentTrick().isComplete(3)) {
            final var winner = gameState.getCurrentTrick().getWinner(
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

        gameRepository.save(gameState);
    }

    private void calculateScores(final GameState gameState) {
        final var declarer = gameState.getDeclarer();
        final var tricksNeeded = gameState.getContract().getTricks();
        final var tricksTaken = gameState.getTricksWon().get(declarer);

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
            for (final var player : gameState.getPlayers()) {
                if (!player.equals(declarer)) {
                    final var tricks = gameState.getTricksWon().get(player);
                    player.addScore(tricks);
                }
            }
        }
    }

    public void startNextRound(final String gameId) {
        final var gameState = getGame(gameId);
        if (gameState == null) {
            throw new IllegalArgumentException("Game not found");
        }
        if (gameState.getPhase() != GamePhase.ROUND_COMPLETE) {
            throw new IllegalStateException("Round is not complete");
        }

        gameState.nextRound();
        startRound(gameState);
        gameRepository.save(gameState);
    }

    private Player findPlayer(final GameState gameState, final String username) {
        return gameState.getPlayers().stream()
                .filter(p -> p.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not in game: " + username));
    }
}
