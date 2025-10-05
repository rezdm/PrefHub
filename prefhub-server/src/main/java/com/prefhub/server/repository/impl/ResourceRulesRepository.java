package com.prefhub.server.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prefhub.core.model.GameRules;
import com.prefhub.server.repository.RulesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RulesRepository implementation that loads rules from classpath resources.
 * Used in test/scenario mode where rules are read-only from resources.
 */
public class ResourceRulesRepository implements RulesRepository {
    private static final Logger logger = LoggerFactory.getLogger(ResourceRulesRepository.class);
    private final ObjectMapper objectMapper;
    private final String resourcePath;

    public ResourceRulesRepository(final String resourcePath) {
        this.resourcePath = resourcePath;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void save(final String ruleId, final GameRules rules) {
        throw new UnsupportedOperationException("Cannot save rules in resource-based repository");
    }

    @Override
    public Optional<GameRules> findById(final String ruleId) {
        final var resourceName = resourcePath + "/" + ruleId + ".json";
        try (final var inputStream = getClass().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                logger.warn("Rules resource not found: {}", resourceName);
                return Optional.empty();
            }
            final var rules = objectMapper.readValue(inputStream, GameRules.class);
            logger.debug("Loaded rules from resource: {}", ruleId);
            return Optional.of(rules);
        } catch (IOException e) {
            logger.error("Failed to load rules from resource: {}", resourceName, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean exists(final String ruleId) {
        return findById(ruleId).isPresent();
    }

    @Override
    public void delete(final String ruleId) {
        throw new UnsupportedOperationException("Cannot delete rules in resource-based repository");
    }

    @Override
    public List<RuleEntry> findAll() {
        final List<RuleEntry> rules = new ArrayList<>();

        // Load known rule files from resources
        final String[] knownRules = {"sochinka", "rostov", "leningrad"};

        for (final var ruleId : knownRules) {
            findById(ruleId).ifPresent(gameRules ->
                rules.add(new RuleEntry(ruleId, gameRules))
            );
        }

        logger.info("Loaded {} rules from resources", rules.size());
        return rules;
    }
}
