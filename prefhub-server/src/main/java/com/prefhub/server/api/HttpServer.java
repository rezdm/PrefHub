package com.prefhub.server.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prefhub.server.auth.AuthService;
import com.prefhub.server.game.GameService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class HttpServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
    private static final Logger accessLogger = LoggerFactory.getLogger("com.prefhub.server.api.ACCESS");

    private final com.sun.net.httpserver.HttpServer server;
    private final ObjectMapper objectMapper;
    private final AuthService authService;
    private final GameService gameService;

    public HttpServer(final int port, final AuthService authService, final GameService gameService) throws IOException {
        logger.info("Initializing HTTP server on port {}", port);

        // Get all IPv4 addresses and bind to first one (usually 0.0.0.0)
        InetAddress ipv4Address = null;
        for (final var addr : InetAddress.getAllByName("0.0.0.0")) {
            if (addr instanceof Inet4Address) {
                ipv4Address = addr;
                break;
            }
        }

        if (ipv4Address == null) {
            ipv4Address = Inet4Address.getByName("0.0.0.0");
        }

        logger.debug("Binding to address: {}", ipv4Address.getHostAddress());
        this.server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(ipv4Address, port), 0);
        this.objectMapper = new ObjectMapper();
        this.authService = authService;
        this.gameService = gameService;

        setupRoutes();
        logger.info("HTTP server initialized successfully");
    }

    private void setupRoutes() {
        // Static file serving
        server.createContext("/", new StaticFileHandler());

        // API routes
        server.createContext("/api/auth/register", new RegisterHandler());
        server.createContext("/api/auth/login", new LoginHandler());
        server.createContext("/api/auth/logout", new LogoutHandler());

        server.createContext("/api/games/create", new CreateGameHandler());
        server.createContext("/api/games/join", new JoinGameHandler());
        server.createContext("/api/games/list", new ListGamesHandler());
        server.createContext("/api/games/state", new GameStateHandler());
        server.createContext("/api/games/bid", new PlaceBidHandler());
        server.createContext("/api/games/exchange", new ExchangeWidowHandler());
        server.createContext("/api/games/play", new PlayCardHandler());
        server.createContext("/api/games/next-round", new NextRoundHandler());
    }

    public void start() {
        server.start();
        logger.info("Server started on port {}", server.getAddress().getPort());
        logger.info("Web interface available at http://localhost:{}/", server.getAddress().getPort());
    }

    public void stop() {
        logger.info("Stopping HTTP server");
        server.stop(0);
        logger.info("HTTP server stopped");
    }

    private void sendResponse(final HttpExchange exchange, final int statusCode, final String response) throws IOException {
        final var bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (final var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(final HttpExchange exchange, final int statusCode, final String message) throws IOException {
        final var json = String.format("{\"error\":\"%s\"}", message);
        sendResponse(exchange, statusCode, json);
    }

    private String getAuthToken(final HttpExchange exchange) {
        final var auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    // Handlers
    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            var path = exchange.getRequestURI().getPath();
            final var clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();

            // Default to index.html for root path
            if (path.equals("/")) {
                path = "/index.html";
            }

            // Serve file from resources/static
            try (final var is = getClass().getResourceAsStream("/static" + path)) {
                if (is == null) {
                    accessLogger.info("{} {} {} - 404", clientIp, exchange.getRequestMethod(), exchange.getRequestURI().getPath());
                    logger.debug("File not found: {}", path);
                    final var notFound = "404 Not Found";
                    exchange.sendResponseHeaders(404, notFound.length());
                    try (final var os = exchange.getResponseBody()) {
                        os.write(notFound.getBytes());
                    }
                    return;
                }

                // Determine content type
                final var contentType = getContentType(path);
                exchange.getResponseHeaders().set("Content-Type", contentType);

                // Read and send file
                final var bytes = is.readAllBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (final var os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                accessLogger.info("{} {} {} - 200", clientIp, exchange.getRequestMethod(), exchange.getRequestURI().getPath());
            }
        }

        private String getContentType(final String path) {
            if (path.endsWith(".html")) return "text/html; charset=UTF-8";
            if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (path.endsWith(".css")) return "text/css; charset=UTF-8";
            if (path.endsWith(".json")) return "application/json";
            return "text/plain";
        }
    }

    private class RegisterHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            final var clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
            accessLogger.info("{} {} {}", clientIp, exchange.getRequestMethod(), exchange.getRequestURI().getPath());

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            try {
                final var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                final var request = objectMapper.readValue(body, java.util.Map.class);
                final var username = (String) request.get("username");
                final var password = (String) request.get("password");

                authService.register(username, password);
                logger.info("New user registered: {}", username);
                sendResponse(exchange, 200, "{\"message\":\"User registered successfully\"}");
            } catch (Exception e) {
                logger.warn("Registration failed: {}", e.getMessage());
                sendError(exchange, 400, e.getMessage());
            }
        }
    }

    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            final var clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
            accessLogger.info("{} {} {}", clientIp, exchange.getRequestMethod(), exchange.getRequestURI().getPath());

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            try {
                final var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                final var request = objectMapper.readValue(body, java.util.Map.class);
                final var username = (String) request.get("username");
                final var password = (String) request.get("password");

                final var token = authService.login(username, password);
                logger.info("User logged in: {} from {}", username, clientIp);
                final var json = objectMapper.writeValueAsString(java.util.Map.of("token", token));
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                logger.warn("Login failed from {}: {}", clientIp, e.getMessage());
                sendError(exchange, 401, e.getMessage());
            }
        }
    }

    private class LogoutHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            final var token = getAuthToken(exchange);
            if (token == null) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }

            authService.logout(token);
            sendResponse(exchange, 200, "{\"message\":\"Logged out successfully\"}");
        }
    }

    private class CreateGameHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            final var clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
            accessLogger.info("{} {} {}", clientIp, exchange.getRequestMethod(), exchange.getRequestURI().getPath());

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            final var token = getAuthToken(exchange);
            if (!authService.isAuthenticated(token)) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }

            try {
                final var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                final var request = objectMapper.readValue(body, java.util.Map.class);
                final var gameId = (String) request.get("gameId");

                final var gameState = gameService.createGame(gameId);
                logger.info("Game created: {}", gameId);
                final var json = objectMapper.writeValueAsString(gameState);
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                logger.error("Failed to create game: {}", e.getMessage(), e);
                sendError(exchange, 400, e.getMessage());
            }
        }
    }

    private class JoinGameHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            final var clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
            accessLogger.info("{} {} {}", clientIp, exchange.getRequestMethod(), exchange.getRequestURI().getPath());

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            final var token = getAuthToken(exchange);
            final var username = authService.validateToken(token);
            if (username == null) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }

            try {
                final var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                final var request = objectMapper.readValue(body, java.util.Map.class);
                final var gameId = (String) request.get("gameId");

                final var gameState = gameService.joinGame(gameId, username);
                logger.info("Player {} joined game: {}", username, gameId);
                final var json = objectMapper.writeValueAsString(gameState);
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                logger.error("Failed to join game: {}", e.getMessage());
                sendError(exchange, 400, e.getMessage());
            }
        }
    }

    private class ListGamesHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            final var token = getAuthToken(exchange);
            if (!authService.isAuthenticated(token)) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }

            final var games = gameService.getAllGames();
            final var json = objectMapper.writeValueAsString(games);
            sendResponse(exchange, 200, json);
        }
    }

    private class GameStateHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            final var token = getAuthToken(exchange);
            final var username = authService.validateToken(token);
            if (username == null) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }

            final var query = exchange.getRequestURI().getQuery();
            final var gameId = query != null && query.startsWith("gameId=") ? query.substring(7) : null;

            if (gameId == null) {
                sendError(exchange, 400, "Missing gameId parameter");
                return;
            }

            try {
                // Return player-specific view instead of full game state
                final var playerView = gameService.getPlayerView(gameId, username);
                final var json = objectMapper.writeValueAsString(playerView);
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                sendError(exchange, 404, e.getMessage());
            }
        }
    }

    private class PlaceBidHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            final var clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
            accessLogger.info("{} {} {}", clientIp, exchange.getRequestMethod(), exchange.getRequestURI().getPath());

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            final var token = getAuthToken(exchange);
            final var username = authService.validateToken(token);
            if (username == null) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }

            try {
                final var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                final var request = objectMapper.readValue(body, java.util.Map.class);
                final var gameId = (String) request.get("gameId");
                final var contractStr = (String) request.get("contract");

                gameService.placeBid(gameId, username, com.prefhub.core.model.Contract.valueOf(contractStr));
                logger.debug("Player {} placed bid {} in game {}", username, contractStr, gameId);
                sendResponse(exchange, 200, "{\"message\":\"Bid placed successfully\"}");
            } catch (Exception e) {
                logger.error("Failed to place bid: {}", e.getMessage());
                sendError(exchange, 400, e.getMessage());
            }
        }
    }

    private class ExchangeWidowHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            final var token = getAuthToken(exchange);
            final var username = authService.validateToken(token);
            if (username == null) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }

            try {
                final var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                final var request = objectMapper.readValue(body, java.util.Map.class);
                final var gameId = (String) request.get("gameId");
                // Parse cards to discard

                sendResponse(exchange, 200, "{\"message\":\"Widow exchanged successfully\"}");
            } catch (Exception e) {
                sendError(exchange, 400, e.getMessage());
            }
        }
    }

    private class PlayCardHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            final var token = getAuthToken(exchange);
            final var username = authService.validateToken(token);
            if (username == null) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }

            try {
                final var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                final var request = objectMapper.readValue(body, java.util.Map.class);
                final var gameId = (String) request.get("gameId");
                // Parse card to play

                sendResponse(exchange, 200, "{\"message\":\"Card played successfully\"}");
            } catch (Exception e) {
                sendError(exchange, 400, e.getMessage());
            }
        }
    }

    private class NextRoundHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            final var token = getAuthToken(exchange);
            if (!authService.isAuthenticated(token)) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }

            try {
                final var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                final var request = objectMapper.readValue(body, java.util.Map.class);
                final var gameId = (String) request.get("gameId");

                gameService.startNextRound(gameId);
                sendResponse(exchange, 200, "{\"message\":\"Next round started\"}");
            } catch (Exception e) {
                sendError(exchange, 400, e.getMessage());
            }
        }
    }
}
