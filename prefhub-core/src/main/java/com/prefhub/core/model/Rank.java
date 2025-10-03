package com.prefhub.core.model;

public enum Rank {
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "В", "Валет"),
    QUEEN(12, "Д", "Дама"),
    KING(13, "К", "Король"),
    ACE(14, "Т", "Туз");

    private final int value;
    private final String symbol;
    private final String russianName;

    Rank(int value, String symbol) {
        this(value, symbol, symbol);
    }

    Rank(int value, String symbol, String russianName) {
        this.value = value;
        this.symbol = symbol;
        this.russianName = russianName;
    }

    public int getValue() {
        return value;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getRussianName() {
        return russianName;
    }
}
