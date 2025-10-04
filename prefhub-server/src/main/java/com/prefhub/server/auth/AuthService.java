package com.prefhub.server.auth;


import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> sessions = new ConcurrentHashMap<>(); // token -> username

    public synchronized void register(final String username, final String password) {
        if (users.containsKey(username)) {
            throw new IllegalArgumentException("User already exists");
        }
        users.put(username, new User(username, User.hashPassword(password)));
    }

    public String login(final String username, final String password) {
        final var user = users.get(username);
        if (user == null || !user.verifyPassword(password)) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        final var token = UUID.randomUUID().toString();
        sessions.put(token, username);
        return token;
    }

    public void logout(final String token) {
        sessions.remove(token);
    }

    public String validateToken(final String token) {
        return sessions.get(token);
    }

    public boolean isAuthenticated(final String token) {
        return sessions.containsKey(token);
    }
}
