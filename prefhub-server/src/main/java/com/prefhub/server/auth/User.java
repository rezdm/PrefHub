package com.prefhub.server.auth;

import java.util.Objects;

public record User(String username, String passwordHash) {
    public boolean verifyPassword(String password) {
        // Простая проверка (в продакшене использовать BCrypt или подобное)
        return Objects.equals(hashPassword(password), passwordHash);
    }

    public static String hashPassword(String password) {
        // Упрощенный хеш для демо (в продакшене использовать BCrypt)
        return String.valueOf(password.hashCode());
    }
}
