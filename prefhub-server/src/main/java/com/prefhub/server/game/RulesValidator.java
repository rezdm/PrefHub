package com.prefhub.server.game;

import com.prefhub.core.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Валидатор действий игроков согласно правилам игры
 */
public class RulesValidator {

    /**
     * Валидация заявки (бида) игрока
     */
    public static void validateBid(final GameState gameState, final Contract bid) {
        final var rules = gameState.getRules();

        if (bid.isPass()) {
            return; // Пас всегда разрешён
        }

        // Проверка минимальной заявки
        if (!bid.isMiser() && bid.getTricks() < rules.getMinimumOpeningBid()) {
            throw new IllegalArgumentException(
                "Минимальная заявка: " + rules.getMinimumOpeningBid());
        }

        // TODO: Проверка тёмной игры и рефа
        // Эти функции будут реализованы позже как модификаторы контрактов

        // Проверка, что заявка выше текущей максимальной
        final var bids = gameState.getBids();
        Contract maxBid = null;
        for (final var existingBid : bids.values()) {
            if (!existingBid.isPass() && (maxBid == null || existingBid.isHigherThan(maxBid))) {
                maxBid = existingBid;
            }
        }

        if (maxBid != null && !bid.isHigherThan(maxBid)) {
            throw new IllegalArgumentException(
                "Заявка должна быть выше текущей: " + maxBid.getDisplayName());
        }
    }

    /**
     * Валидация виста
     */
    public static void validateWhist(final GameState gameState, final Player player,
                                     final boolean isWhisting, final boolean isHalfWhist) {
        final var rules = gameState.getRules();
        final var contract = gameState.getContract();

        if (contract == null) {
            throw new IllegalStateException("Нет контракта для виста");
        }

        // Проверка обязательного виста на 6 пик
        if (rules.isMandatory6SpadesWhist() &&
            contract.getTricks() == 6 &&
            contract.getTrumpSuit() == Suit.SPADES &&
            !isWhisting) {
            throw new IllegalArgumentException("Вист на 6 пик обязателен");
        }

        // Проверка пол-виста
        if (isHalfWhist && !rules.isAllowHalfWhist()) {
            throw new IllegalArgumentException("Пол-вист запрещён в этом варианте правил");
        }

        // Проверка пол-виста после паса
        if (isHalfWhist && !rules.isAllowHalfWhistAfterPass()) {
            // Нужно проверить, был ли уже пас от другого игрока
            final var bids = gameState.getBids();
            final var hasPass = bids.entrySet().stream()
                .filter(e -> !e.getKey().equals(gameState.getDeclarer()))
                .anyMatch(e -> e.getValue().isPass());

            if (hasPass) {
                throw new IllegalArgumentException(
                    "Пол-вист после паса запрещён в этом варианте правил");
            }
        }
    }

    /**
     * Получить список доступных контрактов для заявки
     */
    public static List<Contract> getAvailableBids(final GameState gameState) {
        final var rules = gameState.getRules();
        final var available = new ArrayList<Contract>();

        // Найти максимальную текущую заявку
        Contract maxBid = null;
        for (final var bid : gameState.getBids().values()) {
            if (!bid.isPass() && (maxBid == null || bid.isHigherThan(maxBid))) {
                maxBid = bid;
            }
        }

        // Пас всегда доступен
        available.add(Contract.PASS);

        // Генерируем доступные игровые контракты
        for (final var contract : Contract.values()) {
            // Пропускаем пас (уже добавлен)
            if (contract.isPass()) {
                continue;
            }

            // Проверка минимальной заявки
            if (maxBid == null && contract.getTricks() < rules.getMinimumOpeningBid() && !contract.isMiser()) {
                continue;
            }

            // Проверка, что заявка выше текущей
            if (maxBid != null && !contract.isHigherThan(maxBid)) {
                continue;
            }

            available.add(contract);
        }

        return available;
    }

    /**
     * Валидация выхода из распасов
     */
    public static boolean canExitMizer(final GameState gameState, final Player player) {
        final var rules = gameState.getRules();

        // Получить количество распасов у игрока
        final int mizerCount = player.getMountain(); // TODO: нужно отдельное поле для распасов

        if (mizerCount == 0) {
            return true; // Нет распасов
        }

        // Определить требуемый контракт для выхода
        final int requiredTricks;
        switch (rules.getMizerExitType()) {
            case FLAT_6:
                requiredTricks = 6;
                break;
            case ESCALATING_678:
                if (mizerCount == 1) requiredTricks = 6;
                else if (mizerCount == 2) requiredTricks = 7;
                else requiredTricks = 8;
                break;
            case ESCALATING_677:
                if (mizerCount == 1) requiredTricks = 6;
                else requiredTricks = 7;
                break;
            default:
                requiredTricks = 6;
        }

        // Проверка разрешения "выход без трёх"
        if (!rules.isAllowExitWithoutThree()) {
            // Игрок должен взять минимум 3 взятки сверх контракта
            // Это нужно будет проверить после розыгрыша
        }

        return true; // Временно разрешаем
    }

    /**
     * Получить описание правил для клиента
     */
    public static String getRulesDescription(final GameRules rules) {
        final var sb = new StringBuilder();

        sb.append("Правила: ").append(rules.getName()).append("\n");
        sb.append(rules.getDescription()).append("\n\n");

        sb.append("Выход из распасов: ");
        switch (rules.getMizerExitType()) {
            case FLAT_6 -> sb.append("6-6-6...");
            case ESCALATING_678 -> sb.append("6-7-8-8-8...");
            case ESCALATING_677 -> sb.append("6-7-7-7-7...");
        }
        sb.append("\n");

        if (rules.isMandatory6SpadesWhist()) {
            sb.append("6 пик - обязательный вист\n");
        }

        if (rules.isAllowHalfWhist()) {
            sb.append("Разрешён пол-вист\n");
        }

        sb.append("Десятерная: ");
        sb.append(rules.getTenGameMode() == GameRules.TenGameMode.CHECKED ?
            "проверяется" : "вистуется");
        sb.append("\n");

        sb.append("Вист: ");
        sb.append(rules.getWhistType() == GameRules.WhistType.OPEN ?
            "открытый" : "закрытый");
        sb.append("\n");

        if (rules.isPoolEnabled()) {
            sb.append("Пуля включена (размер: ").append(rules.getPoolSize()).append(")\n");
        }

        return sb.toString();
    }
}
