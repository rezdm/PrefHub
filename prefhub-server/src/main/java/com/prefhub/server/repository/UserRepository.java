package com.prefhub.server.repository;

import com.prefhub.server.auth.User;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity persistence
 */
public interface UserRepository {
    /**
     * Save or update a user
     */
    void save(User user);

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if user exists
     */
    boolean exists(String username);

    /**
     * Delete a user
     */
    void delete(String username);

    /**
     * Get all users
     */
    List<User> findAll();
}
