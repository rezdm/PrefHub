package com.prefhub.server.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prefhub.core.model.GameState;
import com.prefhub.server.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * File-based implementation of GameRepository using JSON
 */
public class FileGameRepository implements GameRepository {
    private static final Logger logger = LoggerFactory.getLogger(FileGameRepository.class);
    private final Path storageDirectory;
    private final ObjectMapper objectMapper;

    public FileGameRepository(final String storageDirectory) {
        this.storageDirectory = Paths.get(storageDirectory, "games");
        this.objectMapper = new ObjectMapper();
        try {
            Files.createDirectories(this.storageDirectory);
            logger.info("Game storage directory: {}", this.storageDirectory.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create game storage directory", e);
        }
    }

    @Override
    public void save(final GameState gameState) {
        final var file = getGameFile(gameState.getGameId());
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, gameState);
            logger.debug("Game saved: {}", gameState.getGameId());
        } catch (IOException e) {
            logger.error("Failed to save game: {}", gameState.getGameId(), e);
            throw new RuntimeException("Failed to save game: " + gameState.getGameId(), e);
        }
    }

    @Override
    public Optional<GameState> findById(final String gameId) {
        final var file = getGameFile(gameId);
        if (!file.exists()) {
            return Optional.empty();
        }
        try {
            final var gameState = objectMapper.readValue(file, GameState.class);
            logger.debug("Game loaded: {}", gameId);
            return Optional.of(gameState);
        } catch (IOException e) {
            logger.error("Failed to load game: {}", gameId, e);
            throw new RuntimeException("Failed to load game: " + gameId, e);
        }
    }

    @Override
    public boolean exists(final String gameId) {
        return getGameFile(gameId).exists();
    }

    @Override
    public void delete(final String gameId) {
        final var file = getGameFile(gameId);
        if (file.exists()) {
            final var deleted = file.delete();
            logger.debug("Game deleted: {} (success: {})", gameId, deleted);
        }
    }

    @Override
    public List<GameState> findAll() {
        final List<GameState> games = new ArrayList<>();
        final var dir = storageDirectory.toFile();

        if (!dir.exists() || !dir.isDirectory()) {
            return games;
        }

        try (Stream<Path> paths = Files.list(storageDirectory)) {
            paths.filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        games.add(objectMapper.readValue(path.toFile(), GameState.class));
                    } catch (IOException e) {
                        logger.error("Failed to load game from file: {}", path, e);
                    }
                });
        } catch (IOException e) {
            logger.error("Failed to list game files", e);
        }

        logger.info("Loaded {} games from storage", games.size());
        return games;
    }

    private File getGameFile(final String gameId) {
        return storageDirectory.resolve(gameId + ".json").toFile();
    }
}
