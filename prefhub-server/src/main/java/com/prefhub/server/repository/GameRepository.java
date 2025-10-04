package com.prefhub.server.repository;

import com.prefhub.core.model.GameState;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for GameState entity persistence
 */
public interface GameRepository {
    /**
     * Save or update a game
     */
    void save(GameState gameState);

    /**
     * Find game by ID
     */
    Optional<GameState> findById(String gameId);

    /**
     * Check if game exists
     */
    boolean exists(String gameId);

    /**
     * Delete a game
     */
    void delete(String gameId);

    /**
     * Get all games
     */
    List<GameState> findAll();
}
