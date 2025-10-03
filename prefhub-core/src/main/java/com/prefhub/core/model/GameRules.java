package com.prefhub.core.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.io.Serial;

/**
 * Конфигурация правил игры в преферанс
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.ANY,
                setterVisibility = JsonAutoDetect.Visibility.ANY)
public class GameRules implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Название варианта правил (например, "Сочинка", "Ленинградка")
     */
    private String name;

    /**
     * Описание варианта правил
     */
    private String description;

    // === Правила распасов (мизеров) ===

    /**
     * Тип выхода из распасов
     */
    public enum MizerExitType {
        FLAT_6,        // 6, 6, 6, 6, ...
        ESCALATING_678, // 6, 7, 8, 8, 8, ...
        ESCALATING_677  // 6, 7, 7, 7, 7, ...
    }

    private MizerExitType mizerExitType;

    /**
     * Считается ли выход из распасов если заказанная игра не сыграна
     */
    private boolean mizerExitOnFailedContract;

    /**
     * Разрешать выход из распасов без трёх
     */
    private boolean allowExitWithoutThree;

    // === Правила торговли ===

    /**
     * Обязательный вист на 6 пик
     */
    private boolean mandatory6SpadesWhist;

    /**
     * Разрешать пол-виста (половинный вист)
     */
    private boolean allowHalfWhist;

    /**
     * Разрешать рефа (повторная игра)
     */
    private boolean allowRefas;

    /**
     * Разрешать тёмную игру (игра без просмотра прикупа)
     */
    private boolean allowDarkGame;

    /**
     * Минимальный контракт для начала торговли
     */
    private int minimumOpeningBid;

    // === Правила десятерной ===

    /**
     * Режим игры на десять взяток
     */
    public enum TenGameMode {
        CHECKED,  // Проверяется (соло, оба защитника обязаны проверять)
        WHISTED   // Вистуется (защитники вистуют по желанию)
    }

    private TenGameMode tenGameMode;

    // === Правила виста ===

    /**
     * Тип виста
     */
    public enum WhistType {
        OPEN,   // Открытый вист (карты на столе)
        CLOSED  // Закрытый вист (карты в руке)
    }

    private WhistType whistType;

    /**
     * Разрешать полувист после паса одного из игроков
     */
    private boolean allowHalfWhistAfterPass;

    // === Правила пули (пулька) ===

    /**
     * Включена ли пуля
     */
    private boolean poolEnabled;

    /**
     * Размер пули в очках
     */
    private int poolSize;

    /**
     * Стоимость пули в очках при разыгрывании
     */
    private int poolValue;

    // === Правила бескозырки ===

    /**
     * Приоритет бескозырки над козырными контрактами одного уровня
     */
    private boolean sansAtoutPriority;

    // === Правила подсчёта очков ===

    /**
     * Множитель очков за каждую взятку при игре
     */
    private int trickPointsMultiplier;

    /**
     * Штраф за недобор
     */
    private int undertrickPenalty;

    /**
     * Премия за точное выполнение контракта
     */
    private int exactContractBonus;

    /**
     * Использовать запись в пулю
     */
    private boolean usePoolRecording;

    /**
     * Количество очков для записи в пулю
     */
    private int poolRecordingThreshold;

    // === Прочие правила ===

    /**
     * Разрешать договорные игры (сговор между игроками)
     */
    private boolean allowAgreements;

    /**
     * Время на ход (в секундах, 0 = без ограничения)
     */
    private int moveTimeLimit;

    /**
     * Время на торговлю (в секундах, 0 = без ограничения)
     */
    private int biddingTimeLimit;

    // Constructors

    public GameRules() {
        // Default values
        this.name = "Default";
        this.description = "Стандартные правила";
        this.mizerExitType = MizerExitType.FLAT_6;
        this.mizerExitOnFailedContract = false;
        this.allowExitWithoutThree = false;
        this.mandatory6SpadesWhist = true;
        this.allowHalfWhist = true;
        this.allowRefas = true;
        this.allowDarkGame = false;
        this.minimumOpeningBid = 6;
        this.tenGameMode = TenGameMode.CHECKED;
        this.whistType = WhistType.CLOSED;
        this.allowHalfWhistAfterPass = true;
        this.poolEnabled = false;
        this.poolSize = 0;
        this.poolValue = 0;
        this.sansAtoutPriority = true;
        this.trickPointsMultiplier = 2;
        this.undertrickPenalty = 2;
        this.exactContractBonus = 0;
        this.usePoolRecording = false;
        this.poolRecordingThreshold = 10;
        this.allowAgreements = false;
        this.moveTimeLimit = 0;
        this.biddingTimeLimit = 0;
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MizerExitType getMizerExitType() {
        return mizerExitType;
    }

    public void setMizerExitType(MizerExitType mizerExitType) {
        this.mizerExitType = mizerExitType;
    }

    public boolean isMizerExitOnFailedContract() {
        return mizerExitOnFailedContract;
    }

    public void setMizerExitOnFailedContract(boolean mizerExitOnFailedContract) {
        this.mizerExitOnFailedContract = mizerExitOnFailedContract;
    }

    public boolean isAllowExitWithoutThree() {
        return allowExitWithoutThree;
    }

    public void setAllowExitWithoutThree(boolean allowExitWithoutThree) {
        this.allowExitWithoutThree = allowExitWithoutThree;
    }

    public boolean isMandatory6SpadesWhist() {
        return mandatory6SpadesWhist;
    }

    public void setMandatory6SpadesWhist(boolean mandatory6SpadesWhist) {
        this.mandatory6SpadesWhist = mandatory6SpadesWhist;
    }

    public boolean isAllowHalfWhist() {
        return allowHalfWhist;
    }

    public void setAllowHalfWhist(boolean allowHalfWhist) {
        this.allowHalfWhist = allowHalfWhist;
    }

    public boolean isAllowRefas() {
        return allowRefas;
    }

    public void setAllowRefas(boolean allowRefas) {
        this.allowRefas = allowRefas;
    }

    public boolean isAllowDarkGame() {
        return allowDarkGame;
    }

    public void setAllowDarkGame(boolean allowDarkGame) {
        this.allowDarkGame = allowDarkGame;
    }

    public int getMinimumOpeningBid() {
        return minimumOpeningBid;
    }

    public void setMinimumOpeningBid(int minimumOpeningBid) {
        this.minimumOpeningBid = minimumOpeningBid;
    }

    public TenGameMode getTenGameMode() {
        return tenGameMode;
    }

    public void setTenGameMode(TenGameMode tenGameMode) {
        this.tenGameMode = tenGameMode;
    }

    public WhistType getWhistType() {
        return whistType;
    }

    public void setWhistType(WhistType whistType) {
        this.whistType = whistType;
    }

    public boolean isAllowHalfWhistAfterPass() {
        return allowHalfWhistAfterPass;
    }

    public void setAllowHalfWhistAfterPass(boolean allowHalfWhistAfterPass) {
        this.allowHalfWhistAfterPass = allowHalfWhistAfterPass;
    }

    public boolean isPoolEnabled() {
        return poolEnabled;
    }

    public void setPoolEnabled(boolean poolEnabled) {
        this.poolEnabled = poolEnabled;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getPoolValue() {
        return poolValue;
    }

    public void setPoolValue(int poolValue) {
        this.poolValue = poolValue;
    }

    public boolean isSansAtoutPriority() {
        return sansAtoutPriority;
    }

    public void setSansAtoutPriority(boolean sansAtoutPriority) {
        this.sansAtoutPriority = sansAtoutPriority;
    }

    public int getTrickPointsMultiplier() {
        return trickPointsMultiplier;
    }

    public void setTrickPointsMultiplier(int trickPointsMultiplier) {
        this.trickPointsMultiplier = trickPointsMultiplier;
    }

    public int getUndertrickPenalty() {
        return undertrickPenalty;
    }

    public void setUndertrickPenalty(int undertrickPenalty) {
        this.undertrickPenalty = undertrickPenalty;
    }

    public int getExactContractBonus() {
        return exactContractBonus;
    }

    public void setExactContractBonus(int exactContractBonus) {
        this.exactContractBonus = exactContractBonus;
    }

    public boolean isUsePoolRecording() {
        return usePoolRecording;
    }

    public void setUsePoolRecording(boolean usePoolRecording) {
        this.usePoolRecording = usePoolRecording;
    }

    public int getPoolRecordingThreshold() {
        return poolRecordingThreshold;
    }

    public void setPoolRecordingThreshold(int poolRecordingThreshold) {
        this.poolRecordingThreshold = poolRecordingThreshold;
    }

    public boolean isAllowAgreements() {
        return allowAgreements;
    }

    public void setAllowAgreements(boolean allowAgreements) {
        this.allowAgreements = allowAgreements;
    }

    public int getMoveTimeLimit() {
        return moveTimeLimit;
    }

    public void setMoveTimeLimit(int moveTimeLimit) {
        this.moveTimeLimit = moveTimeLimit;
    }

    public int getBiddingTimeLimit() {
        return biddingTimeLimit;
    }

    public void setBiddingTimeLimit(int biddingTimeLimit) {
        this.biddingTimeLimit = biddingTimeLimit;
    }

    @Override
    public String toString() {
        return "GameRules{" +
                "name='" + name + '\'' +
                ", mizerExitType=" + mizerExitType +
                ", tenGameMode=" + tenGameMode +
                ", whistType=" + whistType +
                '}';
    }
}
