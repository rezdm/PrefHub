package com.prefhub.server.auth;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> sessions = new ConcurrentHashMap<>(); // token -> username

    public synchronized void register(String username, String password) {
        if (users.containsKey(username)) {
            throw new IllegalArgumentException("User already exists");
        }
        users.put(username, new User(username, User.hashPassword(password)));
    }

    public String login(String username, String password) {
        User user = users.get(username);
        if (user == null || !user.verifyPassword(password)) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        String token = UUID.randomUUID().toString();
        sessions.put(token, username);
        return token;
    }

    public void logout(String token) {
        sessions.remove(token);
    }

    public String validateToken(String token) {
        return sessions.get(token);
    }

    public boolean isAuthenticated(String token) {
        return sessions.containsKey(token);
    }
}
