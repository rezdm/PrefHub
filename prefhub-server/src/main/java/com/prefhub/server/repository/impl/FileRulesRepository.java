package com.prefhub.server.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prefhub.core.model.GameRules;
import com.prefhub.server.repository.RulesRepository;
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
 * File-based implementation of RulesRepository using JSON
 */
public class FileRulesRepository implements RulesRepository {
    private static final Logger logger = LoggerFactory.getLogger(FileRulesRepository.class);
    private final Path storageDirectory;
    private final ObjectMapper objectMapper;

    public FileRulesRepository(final String storageDirectory) {
        this.storageDirectory = Paths.get(storageDirectory, "rules");
        this.objectMapper = new ObjectMapper();
        try {
            Files.createDirectories(this.storageDirectory);
            logger.info("Rules storage directory: {}", this.storageDirectory.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create rules storage directory", e);
        }
    }

    @Override
    public void save(final String ruleId, final GameRules rules) {
        final var file = getRuleFile(ruleId);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, rules);
            logger.debug("Rules saved: {}", ruleId);
        } catch (IOException e) {
            logger.error("Failed to save rules: {}", ruleId, e);
            throw new RuntimeException("Failed to save rules: " + ruleId, e);
        }
    }

    @Override
    public Optional<GameRules> findById(final String ruleId) {
        final var file = getRuleFile(ruleId);
        if (!file.exists()) {
            return Optional.empty();
        }
        try {
            final var rules = objectMapper.readValue(file, GameRules.class);
            logger.debug("Rules loaded: {}", ruleId);
            return Optional.of(rules);
        } catch (IOException e) {
            logger.error("Failed to load rules: {}", ruleId, e);
            throw new RuntimeException("Failed to load rules: " + ruleId, e);
        }
    }

    @Override
    public boolean exists(final String ruleId) {
        return getRuleFile(ruleId).exists();
    }

    @Override
    public void delete(final String ruleId) {
        final var file = getRuleFile(ruleId);
        if (file.exists()) {
            final var deleted = file.delete();
            logger.debug("Rules deleted: {} (success: {})", ruleId, deleted);
        }
    }

    @Override
    public List<RuleEntry> findAll() {
        final List<RuleEntry> rules = new ArrayList<>();
        final var dir = storageDirectory.toFile();

        if (!dir.exists() || !dir.isDirectory()) {
            return rules;
        }

        try (Stream<Path> paths = Files.list(storageDirectory)) {
            paths.filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        final var ruleId = path.getFileName().toString().replace(".json", "");
                        final var gameRules = objectMapper.readValue(path.toFile(), GameRules.class);
                        rules.add(new RuleEntry(ruleId, gameRules));
                    } catch (IOException e) {
                        logger.error("Failed to load rules from file: {}", path, e);
                    }
                });
        } catch (IOException e) {
            logger.error("Failed to list rules files", e);
        }

        logger.info("Loaded {} rules from storage", rules.size());
        return rules;
    }

    private File getRuleFile(final String ruleId) {
        return storageDirectory.resolve(ruleId + ".json").toFile();
    }
}
