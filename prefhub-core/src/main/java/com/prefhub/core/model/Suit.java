package com.prefhub.core.model;

public enum Suit {
    SPADES(0, "♠", "Пики"),
    CLUBS(1, "♣", "Трефы"),
    DIAMONDS(2, "♦", "Бубны"),
    HEARTS(3, "♥", "Червы");

    private final int sortOrder;
    private final String symbol;
    private final String russianName;

    Suit(int sortOrder, String symbol, String russianName) {
        this.sortOrder = sortOrder;
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
