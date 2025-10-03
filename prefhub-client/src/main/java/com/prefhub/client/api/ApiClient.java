package com.prefhub.client.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ApiClient {
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String authToken;

    public ApiClient(final String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public void setAuthToken(final String token) {
        this.authToken = token;
    }

    public String getAuthToken() {
        return authToken;
    }

    public Map<String, Object> register(final String username, final String password) throws IOException, InterruptedException {
        final var json = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        return post("/api/auth/register", json, false);
    }

    public Map<String, Object> login(final String username, final String password) throws IOException, InterruptedException {
        final var json = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        final var response = post("/api/auth/login", json, false);
        if (response.containsKey("token")) {
            this.authToken = (String) response.get("token");
        }
        return response;
    }

    public Map<String, Object> logout() throws IOException, InterruptedException {
        return post("/api/auth/logout", "{}", true);
    }

    public Map<String, Object> createGame(final String gameId) throws IOException, InterruptedException {
        final var json = objectMapper.writeValueAsString(Map.of("gameId", gameId));
        return post("/api/games/create", json, true);
    }

    public Map<String, Object> joinGame(final String gameId) throws IOException, InterruptedException {
        final var json = objectMapper.writeValueAsString(Map.of("gameId", gameId));
        return post("/api/games/join", json, true);
    }

    public Object listGames() throws IOException, InterruptedException {
        return get("/api/games/list", true);
    }

    public Map<String, Object> getGameState(final String gameId) throws IOException, InterruptedException {
        return get("/api/games/state?gameId=" + gameId, true);
    }

    public Map<String, Object> placeBid(final String gameId, final String contract) throws IOException, InterruptedException {
        final var json = objectMapper.writeValueAsString(Map.of("gameId", gameId, "contract", contract));
        return post("/api/games/bid", json, true);
    }

    public Map<String, Object> exchangeWidow(final String gameId, final String cards) throws IOException, InterruptedException {
        final var json = objectMapper.writeValueAsString(Map.of("gameId", gameId, "cards", cards));
        return post("/api/games/exchange", json, true);
    }

    public Map<String, Object> playCard(final String gameId, final String card) throws IOException, InterruptedException {
        final var json = objectMapper.writeValueAsString(Map.of("gameId", gameId, "card", card));
        return post("/api/games/play", json, true);
    }

    public Map<String, Object> nextRound(final String gameId) throws IOException, InterruptedException {
        final var json = objectMapper.writeValueAsString(Map.of("gameId", gameId));
        return post("/api/games/next-round", json, true);
    }

    private Map<String, Object> post(final String path, final String body, final boolean auth) throws IOException, InterruptedException {
        final var builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        if (auth && authToken != null) {
            builder.header("Authorization", "Bearer " + authToken);
        }

        final var request = builder.build();
        final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), Map.class);
    }

    private Map<String, Object> get(final String path, final boolean auth) throws IOException, InterruptedException {
        final var builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .GET();

        if (auth && authToken != null) {
            builder.header("Authorization", "Bearer " + authToken);
        }

        final var request = builder.build();
        final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), Map.class);
    }
}
