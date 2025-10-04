package com.prefhub.server.auth;

import java.io.Serializable;
import java.util.Objects;

public record User(String username, String passwordHash) implements Serializable {
    public boolean verifyPassword(String password) {
        return Objects.equals(hashPassword(password), passwordHash);
    }

    public static String hashPassword(String password) {
        return String.valueOf(password.hashCode());
    }
}
