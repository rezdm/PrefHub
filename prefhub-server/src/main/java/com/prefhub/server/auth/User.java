package com.prefhub.server.auth;

import java.util.Objects;

public class User {
    private final String username;
    private final String passwordHash;

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean verifyPassword(String password) {
        // Простая проверка (в продакшене использовать BCrypt или подобное)
        return Objects.equals(hashPassword(password), passwordHash);
    }

    public static String hashPassword(String password) {
        // Упрощенный хеш для демо (в продакшене использовать BCrypt)
        return String.valueOf(password.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final var user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
