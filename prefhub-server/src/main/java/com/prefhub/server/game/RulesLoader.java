package com.prefhub.server.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prefhub.core.model.GameRules;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Загрузчик конфигураций правил игры из classpath
 */
public class RulesLoader {
    private static final String RULES_PATH = "/rules/";
    private static final String[] RULE_FILES = {
        "sochinka.json",
        "leningradka.json",
        "stalingradka.json"
    };

    private final ObjectMapper objectMapper;
    private final Map<String, GameRules> availableRules;

    public RulesLoader() {
        this.objectMapper = new ObjectMapper();
        this.availableRules = new HashMap<>();
        loadAllRules();
    }

    /**
     * Загружает все правила из classpath resources
     */
    private void loadAllRules() {
        for (final var fileName : RULE_FILES) {
            final var resourcePath = RULES_PATH + fileName;
            try (final InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    System.err.println("Rules file not found in classpath: " + resourcePath);
                    continue;
                }

                final var rules = objectMapper.readValue(is, GameRules.class);
                final var ruleId = fileName.replace(".json", "");
                availableRules.put(ruleId, rules);
                System.out.println("Loaded rules: " + rules.getName() + " (ID: " + ruleId + ")");
            } catch (IOException e) {
                System.err.println("Failed to load rules from: " + resourcePath);
                e.printStackTrace();
            }
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
