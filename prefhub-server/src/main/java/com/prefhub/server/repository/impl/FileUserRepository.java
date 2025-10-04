package com.prefhub.server.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prefhub.server.auth.User;
import com.prefhub.server.repository.UserRepository;
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
 * File-based implementation of UserRepository using JSON
 */
public class FileUserRepository implements UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(FileUserRepository.class);
    private final Path storageDirectory;
    private final ObjectMapper objectMapper;

    public FileUserRepository(final String storageDirectory) {
        this.storageDirectory = Paths.get(storageDirectory, "users");
        this.objectMapper = new ObjectMapper();
        try {
            Files.createDirectories(this.storageDirectory);
            logger.info("User storage directory: {}", this.storageDirectory.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create user storage directory", e);
        }
    }

    @Override
    public void save(final User user) {
        final var file = getUserFile(user.username());
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, user);
            logger.debug("User saved: {}", user.username());
        } catch (IOException e) {
            logger.error("Failed to save user: {}", user.username(), e);
            throw new RuntimeException("Failed to save user: " + user.username(), e);
        }
    }

    @Override
    public Optional<User> findByUsername(final String username) {
        final var file = getUserFile(username);
        if (!file.exists()) {
            return Optional.empty();
        }
        try {
            final var user = objectMapper.readValue(file, User.class);
            logger.debug("User loaded: {}", username);
            return Optional.of(user);
        } catch (IOException e) {
            logger.error("Failed to load user: {}", username, e);
            throw new RuntimeException("Failed to load user: " + username, e);
        }
    }

    @Override
    public boolean exists(final String username) {
        return getUserFile(username).exists();
    }

    @Override
    public void delete(final String username) {
        final var file = getUserFile(username);
        if (file.exists()) {
            final var deleted = file.delete();
            logger.debug("User deleted: {} (success: {})", username, deleted);
        }
    }

    @Override
    public List<User> findAll() {
        final List<User> users = new ArrayList<>();
        final var dir = storageDirectory.toFile();

        if (!dir.exists() || !dir.isDirectory()) {
            return users;
        }

        try (Stream<Path> paths = Files.list(storageDirectory)) {
            paths.filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        users.add(objectMapper.readValue(path.toFile(), User.class));
                    } catch (IOException e) {
                        logger.error("Failed to load user from file: {}", path, e);
                    }
                });
        } catch (IOException e) {
            logger.error("Failed to list user files", e);
        }

        logger.info("Loaded {} users from storage", users.size());
        return users;
    }

    private File getUserFile(final String username) {
        return storageDirectory.resolve(username + ".json").toFile();
    }
}
