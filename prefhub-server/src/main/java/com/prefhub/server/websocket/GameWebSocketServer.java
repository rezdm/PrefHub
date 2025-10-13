package com.prefhub.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prefhub.core.model.*;
import com.prefhub.server.auth.AuthService;
import com.prefhub.server.game.GameService;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameWebSocketServer extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketServer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuthService authService;
    private final GameService gameService;

    // Track which user is connected to which WebSocket
    private final Map<WebSocket, String> connectionToUsername = new ConcurrentHashMap<>();
    // Track which game each user is in
    private final Map<String, String> usernameToGameId = new ConcurrentHashMap<>();

    public GameWebSocketServer(int port, AuthService authService, GameService gameService) {
        super(new InetSocketAddress("0.0.0.0", port));
        this.authService = authService;
        this.gameService = gameService;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("New WebSocket connection from {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        final var username = connectionToUsername.remove(conn);
        if (username != null) {
            logger.info("WebSocket connection closed for user: {}", username);
            usernameToGameId.remove(username);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            final var msg = objectMapper.readValue(message, Map.class);
            final var type = (String) msg.get("type");

            switch (type) {
                case "auth" -> handleAuth(conn, msg);
                case "join" -> handleJoinGame(conn, msg);
                case "getState" -> handleGetState(conn);
                case "placeBid" -> handlePlaceBid(conn, msg);
                case "exchangeWidow" -> handleExchangeWidow(conn, msg);
                case "playCard" -> handlePlayCard(conn, msg);
                case "startNextRound" -> handleStartNextRound(conn);
                default -> sendError(conn, "Unknown message type: " + type);
            }
        } catch (Exception e) {
            logger.error("Error handling message", e);
            sendError(conn, "Error: " + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.error("WebSocket error", ex);
    }

    @Override
    public void onStart() {
        logger.info("WebSocket server started on port {}", getPort());
    }

    private void handleAuth(WebSocket conn, Map<String, Object> msg) {
        try {
            final var token = (String) msg.get("token");
            final var username = authService.validateToken(token);

            if (username != null) {
                connectionToUsername.put(conn, username);
                sendMessage(conn, Map.of(
                    "type", "authSuccess",
                    "username", username
                ));
                logger.info("User {} authenticated via WebSocket", username);
            } else {
                sendError(conn, "Invalid token");
            }
        } catch (Exception e) {
            sendError(conn, "Authentication failed: " + e.getMessage());
        }
    }

    private void handleJoinGame(WebSocket conn, Map<String, Object> msg) {
        final var username = connectionToUsername.get(conn);
        if (username == null) {
            sendError(conn, "Not authenticated");
            return;
        }

        try {
            final var gameId = (String) msg.get("gameId");
            gameService.joinGame(gameId, username);
            usernameToGameId.put(username, gameId);

            // Send updated state to all players in the game
            broadcastGameState(gameId);
        } catch (Exception e) {
            sendError(conn, "Failed to join game: " + e.getMessage());
        }
    }

    private void handleGetState(WebSocket conn) {
        final var username = connectionToUsername.get(conn);
        if (username == null) {
            sendError(conn, "Not authenticated");
            return;
        }

        final var gameId = usernameToGameId.get(username);
        if (gameId == null) {
            sendError(conn, "Not in a game");
            return;
        }

        try {
            final var playerView = gameService.getPlayerView(gameId, username);
            sendMessage(conn, Map.of(
                "type", "gameState",
                "state", playerView
            ));
        } catch (Exception e) {
            sendError(conn, "Failed to get game state: " + e.getMessage());
        }
    }

    private void handlePlaceBid(WebSocket conn, Map<String, Object> msg) {
        final var username = connectionToUsername.get(conn);
        if (username == null) {
            sendError(conn, "Not authenticated");
            return;
        }

        final var gameId = usernameToGameId.get(username);
        if (gameId == null) {
            sendError(conn, "Not in a game");
            return;
        }

        try {
            final var contractStr = (String) msg.get("contract");
            final var contract = Contract.valueOf(contractStr);
            gameService.placeBid(gameId, username, contract);

            // Broadcast updated state to all players
            broadcastGameState(gameId);
        } catch (Exception e) {
            sendError(conn, "Failed to place bid: " + e.getMessage());
        }
    }

    private void handleExchangeWidow(WebSocket conn, Map<String, Object> msg) {
        final var username = connectionToUsername.get(conn);
        if (username == null) {
            sendError(conn, "Not authenticated");
            return;
        }

        final var gameId = usernameToGameId.get(username);
        if (gameId == null) {
            sendError(conn, "Not in a game");
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            final var cardMaps = (java.util.List<Map<String, String>>) msg.get("cards");
            final var cards = cardMaps.stream()
                .map(cardMap -> new Card(
                    Suit.valueOf(cardMap.get("suit")),
                    Rank.valueOf(cardMap.get("rank"))
                ))
                .toList();

            gameService.exchangeWidow(gameId, username, cards);

            // Broadcast updated state to all players
            broadcastGameState(gameId);
        } catch (Exception e) {
            sendError(conn, "Failed to exchange widow: " + e.getMessage());
        }
    }

    private void handlePlayCard(WebSocket conn, Map<String, Object> msg) {
        final var username = connectionToUsername.get(conn);
        if (username == null) {
            sendError(conn, "Not authenticated");
            return;
        }

        final var gameId = usernameToGameId.get(username);
        if (gameId == null) {
            sendError(conn, "Not in a game");
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            final var cardMap = (Map<String, String>) msg.get("card");
            final var card = new Card(
                Suit.valueOf(cardMap.get("suit")),
                Rank.valueOf(cardMap.get("rank"))
            );

            gameService.playCard(gameId, username, card);

            // Broadcast updated state to all players
            broadcastGameState(gameId);
        } catch (Exception e) {
            sendError(conn, "Failed to play card: " + e.getMessage());
        }
    }

    private void handleStartNextRound(WebSocket conn) {
        final var username = connectionToUsername.get(conn);
        if (username == null) {
            sendError(conn, "Not authenticated");
            return;
        }

        final var gameId = usernameToGameId.get(username);
        if (gameId == null) {
            sendError(conn, "Not in a game");
            return;
        }

        try {
            gameService.startNextRound(gameId);

            // Broadcast updated state to all players
            broadcastGameState(gameId);
        } catch (Exception e) {
            sendError(conn, "Failed to start next round: " + e.getMessage());
        }
    }

    private void broadcastGameState(String gameId) {
        // Get all players in this game
        final var game = gameService.getGame(gameId);
        if (game == null) {
            return;
        }

        // Send updated state to each player
        for (final var player : game.getPlayers()) {
            final var username = player.getUsername();

            // Find the WebSocket connection for this player
            for (final var entry : connectionToUsername.entrySet()) {
                if (entry.getValue().equals(username)) {
                    try {
                        final var playerView = gameService.getPlayerView(gameId, username);
                        sendMessage(entry.getKey(), Map.of(
                            "type", "gameState",
                            "state", playerView
                        ));
                    } catch (Exception e) {
                        logger.error("Failed to send game state to {}", username, e);
                    }
                    break;
                }
            }
        }
    }

    private void sendMessage(WebSocket conn, Object message) {
        try {
            final var json = objectMapper.writeValueAsString(message);
            conn.send(json);
        } catch (IOException e) {
            logger.error("Failed to send message", e);
        }
    }

    private void sendError(WebSocket conn, String errorMessage) {
        sendMessage(conn, Map.of(
            "type", "error",
            "message", errorMessage
        ));
    }
}
