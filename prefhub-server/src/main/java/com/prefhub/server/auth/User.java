package com.prefhub.server.auth;

import java.util.Objects;

public record User(String username, String passwordHash) {
    public boolean verifyPassword(String password) {
        return Objects.equals(hashPassword(password), passwordHash);
    }

    public static String hashPassword(String password) {
        return String.valueOf(password.hashCode());
    }
}
