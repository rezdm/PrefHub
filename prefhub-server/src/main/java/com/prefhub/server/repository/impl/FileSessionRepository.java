package com.prefhub.server.repository.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prefhub.server.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * File-based implementation of SessionRepository using JSON
 */
public class FileSessionRepository implements SessionRepository {
    private static final Logger logger = LoggerFactory.getLogger(FileSessionRepository.class);
    private final Path storageFile;
    private final ObjectMapper objectMapper;

    public FileSessionRepository(final String storageDirectory) {
        final var dir = Paths.get(storageDirectory, "sessions");
        this.objectMapper = new ObjectMapper();
        try {
            Files.createDirectories(dir);
            this.storageFile = dir.resolve("sessions.json");
            logger.info("Session storage file: {}", this.storageFile.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create session storage directory", e);
        }
    }

    @Override
    public void save(final String token, final String username) {
        final Map<String, String> sessions = loadAll();
        sessions.put(token, username);
        saveAll(sessions);
        logger.debug("Session saved for user: {}", username);
    }

    @Override
    public Optional<String> findUsernameByToken(final String token) {
        final Map<String, String> sessions = loadAll();
        return Optional.ofNullable(sessions.get(token));
    }

    @Override
    public void delete(final String token) {
        final Map<String, String> sessions = loadAll();
        sessions.remove(token);
        saveAll(sessions);
        logger.debug("Session deleted for token: {}", token);
    }

    @Override
    public boolean exists(final String token) {
        return loadAll().containsKey(token);
    }

    @Override
    public Map<String, String> findAll() {
        return new HashMap<>(loadAll());
    }

    private Map<String, String> loadAll() {
        if (!storageFile.toFile().exists()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(storageFile.toFile(), new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            logger.error("Failed to load sessions", e);
            return new HashMap<>();
        }
    }

    private void saveAll(final Map<String, String> sessions) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storageFile.toFile(), sessions);
        } catch (IOException e) {
            logger.error("Failed to save sessions", e);
            throw new RuntimeException("Failed to save sessions", e);
        }
    }
}
