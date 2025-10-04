package com.prefhub.server.repository;

import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for session (token) persistence
 */
public interface SessionRepository {
    /**
     * Save a session
     */
    void save(String token, String username);

    /**
     * Find username by token
     */
    Optional<String> findUsernameByToken(String token);

    /**
     * Delete a session
     */
    void delete(String token);

    /**
     * Check if session exists
     */
    boolean exists(String token);

    /**
     * Get all sessions
     */
    Map<String, String> findAll();
}
