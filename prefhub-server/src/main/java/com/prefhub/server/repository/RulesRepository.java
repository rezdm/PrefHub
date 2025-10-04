package com.prefhub.server.repository;

import com.prefhub.core.model.GameRules;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for game rules persistence
 */
public interface RulesRepository {
    /**
     * Save game rules
     * @param ruleId rule identifier
     * @param rules game rules
     */
    void save(String ruleId, GameRules rules);

    /**
     * Find rules by ID
     * @param ruleId rule identifier
     * @return optional containing rules if found
     */
    Optional<GameRules> findById(String ruleId);

    /**
     * Check if rules exist
     * @param ruleId rule identifier
     * @return true if rules exist
     */
    boolean exists(String ruleId);

    /**
     * Delete rules
     * @param ruleId rule identifier
     */
    void delete(String ruleId);

    /**
     * Get all available rules
     * @return list of all rules with their IDs
     */
    List<RuleEntry> findAll();

    /**
     * Entry containing rule ID and rules
     */
    record RuleEntry(String ruleId, GameRules rules) {}
}
