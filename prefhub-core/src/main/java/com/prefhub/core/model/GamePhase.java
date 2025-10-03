package com.prefhub.core.model;

public enum GamePhase {
    WAITING_FOR_PLAYERS,  // Ожидание игроков
    BIDDING,              // Торговля
    WIDOW_EXCHANGE,       // Обмен с прикупом
    PLAYING,              // Разыгрывание
    ROUND_COMPLETE,       // Раунд завершен
    GAME_COMPLETE         // Игра завершена
}
