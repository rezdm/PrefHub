package com.prefhub.server.persistence;

import com.prefhub.core.model.GameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GamePersistence {
    private static final Logger logger = LoggerFactory.getLogger(GamePersistence.class);
    private final Path storageDirectory;

    public GamePersistence(final String storageDirectory) {
        this.storageDirectory = Paths.get(storageDirectory);

        try {
            Files.createDirectories(this.storageDirectory);
            logger.info("Game storage directory created: {}", this.storageDirectory.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory", e);
        }
    }

    public void saveGame(final GameState gameState) {
        final var file = getGameFile(gameState.getGameId());
        try (final var oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(gameState);
            logger.debug("Game saved: {}", gameState.getGameId());
        } catch (IOException e) {
            logger.error("Failed to save game: {}", gameState.getGameId(), e);
            throw new RuntimeException("Failed to save game: " + gameState.getGameId(), e);
        }
    }

    public GameState loadGame(final String gameId) {
        final var file = getGameFile(gameId);
        if (!file.exists()) {
            logger.debug("Game file not found: {}", gameId);
            return null;
        }
        try (final var ois = new ObjectInputStream(new FileInputStream(file))) {
            final var gameState = (GameState) ois.readObject();
            logger.debug("Game loaded: {}", gameId);
            return gameState;
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Failed to load game: {}", gameId, e);
            throw new RuntimeException("Failed to load game: " + gameId, e);
        }
    }

    public void deleteGame(final String gameId) {
        final var file = getGameFile(gameId);
        if (file.exists()) {
            final var deleted = file.delete();
            logger.debug("Game deleted: {} (success: {})", gameId, deleted);
        }
    }

    private File getGameFile(final String gameId) {
        return storageDirectory.resolve(gameId + ".dat").toFile();
    }
}
