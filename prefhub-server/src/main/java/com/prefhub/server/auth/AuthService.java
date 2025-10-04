package com.prefhub.server.auth;

import com.google.inject.Inject;
import com.prefhub.server.repository.SessionRepository;
import com.prefhub.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    @Inject
    public AuthService(final UserRepository userRepository, final SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        loadExistingData();
    }

    private void loadExistingData() {
        final var users = userRepository.findAll();
        final var sessions = sessionRepository.findAll();
        logger.info("Loaded {} users and {} sessions from storage", users.size(), sessions.size());
    }

    public synchronized void register(final String username, final String password) {
        if (userRepository.exists(username)) {
            throw new IllegalArgumentException("User already exists");
        }
        final var user = new User(username, User.hashPassword(password));
        userRepository.save(user);
        logger.info("User registered: {}", username);
    }

    public String login(final String username, final String password) {
        final var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || !userOpt.get().verifyPassword(password)) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        final var token = UUID.randomUUID().toString();
        sessionRepository.save(token, username);
        logger.info("User logged in: {}", username);
        return token;
    }

    public void logout(final String token) {
        sessionRepository.delete(token);
        logger.info("User logged out");
    }

    public String validateToken(final String token) {
        return sessionRepository.findUsernameByToken(token).orElse(null);
    }

    public boolean isAuthenticated(final String token) {
        return sessionRepository.exists(token);
    }
}
