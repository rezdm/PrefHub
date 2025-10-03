package com.prefhub.core.model;

public enum Suit {
    SPADES("♠", "Пики"),
    CLUBS("♣", "Трефы"),
    DIAMONDS("♦", "Бубны"),
    HEARTS("♥", "Червы");

    private final String symbol;
    private final String russianName;

    Suit(String symbol, String russianName) {
        this.symbol = symbol;
        this.russianName = russianName;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getRussianName() {
        return russianName;
    }
}
