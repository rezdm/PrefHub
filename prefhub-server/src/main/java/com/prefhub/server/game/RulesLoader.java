package com.prefhub.server.game;

import com.prefhub.core.model.GameRules;
import com.google.inject.Inject;
import com.prefhub.server.repository.RulesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Загрузчик конфигураций правил игры из persistence layer
 */
public class RulesLoader {
    private static final Logger logger = LoggerFactory.getLogger(RulesLoader.class);
    private final RulesRepository rulesRepository;
    private final Map<String, GameRules> availableRules;

    @Inject
    public RulesLoader(final RulesRepository rulesRepository) {
        this.rulesRepository = rulesRepository;
        this.availableRules = new HashMap<>();
        loadAllRules();
    }

    /**
     * Загружает все правила из persistence layer
     */
    private void loadAllRules() {
        final var rules = rulesRepository.findAll();
        for (final var entry : rules) {
            availableRules.put(entry.ruleId(), entry.rules());
            logger.info("Loaded rules: {} (ID: {})", entry.rules().getName(), entry.ruleId());
        }
    }

    /**
     * Получить правила по идентификатору
     *
     * @param ruleId идентификатор правил (имя файла без .json)
     * @return правила игры
     * @throws IllegalArgumentException если правила не найдены
     */
    public GameRules getRules(final String ruleId) {
        final var rules = availableRules.get(ruleId);
        if (rules == null) {
            throw new IllegalArgumentException("Rules not found: " + ruleId +
                ". Available: " + String.join(", ", availableRules.keySet()));
        }
        return rules;
    }

    /**
     * Получить список всех доступных правил
     *
     * @return список правил с их идентификаторами
     */
    public Map<String, String> getAvailableRulesList() {
        return availableRules.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().getName() + " - " + e.getValue().getDescription()
            ));
    }

    /**
     * Получить правила по умолчанию (Ленинградка)
     *
     * @return правила игры по умолчанию
     */
    public GameRules getDefaultRules() {
        if (availableRules.containsKey("leningradka")) {
            return availableRules.get("leningradka");
        }
        if (!availableRules.isEmpty()) {
            return availableRules.values().iterator().next();
        }
        return new GameRules(); // Fallback to default constructor
    }

    /**
     * Проверить доступность правил
     *
     * @param ruleId идентификатор правил
     * @return true если правила доступны
     */
    public boolean hasRules(final String ruleId) {
        return availableRules.containsKey(ruleId);
    }
}
