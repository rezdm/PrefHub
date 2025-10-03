package com.prefhub.server.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prefhub.core.model.GameRules;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Загрузчик конфигураций правил игры
 */
public class RulesLoader {
    private final String rulesDirectory;
    private final ObjectMapper objectMapper;
    private final Map<String, GameRules> availableRules;

    public RulesLoader(final String gameDataPath) {
        this.rulesDirectory = gameDataPath + "/rules";
        this.objectMapper = new ObjectMapper();
        this.availableRules = new HashMap<>();
        loadAllRules();
    }

    /**
     * Загружает все правила из директории rules
     */
    private void loadAllRules() {
        final var rulesDir = new File(rulesDirectory);

        if (!rulesDir.exists() || !rulesDir.isDirectory()) {
            System.err.println("Rules directory not found: " + rulesDirectory);
            return;
        }

        final var jsonFiles = rulesDir.listFiles((dir, name) -> name.endsWith(".json"));

        if (jsonFiles == null || jsonFiles.length == 0) {
            System.err.println("No rule files found in: " + rulesDirectory);
            return;
        }

        for (final var file : jsonFiles) {
            try {
                final var rules = objectMapper.readValue(file, GameRules.class);
                final var ruleId = file.getName().replace(".json", "");
                availableRules.put(ruleId, rules);
                System.out.println("Loaded rules: " + rules.getName() + " (ID: " + ruleId + ")");
            } catch (IOException e) {
                System.err.println("Failed to load rules from: " + file.getName());
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
