package com.prefhub.core.model;

public enum Contract {
    // Игровые контракты (от меньшего к большему)
    SIX_SPADES(6, Suit.SPADES, "6♠"),
    SIX_CLUBS(6, Suit.CLUBS, "6♣"),
    SIX_DIAMONDS(6, Suit.DIAMONDS, "6♦"),
    SIX_HEARTS(6, Suit.HEARTS, "6♥"),
    SIX_NO_TRUMP(6, null, "6БК"),

    SEVEN_SPADES(7, Suit.SPADES, "7♠"),
    SEVEN_CLUBS(7, Suit.CLUBS, "7♣"),
    SEVEN_DIAMONDS(7, Suit.DIAMONDS, "7♦"),
    SEVEN_HEARTS(7, Suit.HEARTS, "7♥"),
    SEVEN_NO_TRUMP(7, null, "7БК"),

    EIGHT_SPADES(8, Suit.SPADES, "8♠"),
    EIGHT_CLUBS(8, Suit.CLUBS, "8♣"),
    EIGHT_DIAMONDS(8, Suit.DIAMONDS, "8♦"),
    EIGHT_HEARTS(8, Suit.HEARTS, "8♥"),
    EIGHT_NO_TRUMP(8, null, "8БК"),

    NINE_SPADES(9, Suit.SPADES, "9♠"),
    NINE_CLUBS(9, Suit.CLUBS, "9♣"),
    NINE_DIAMONDS(9, Suit.DIAMONDS, "9♦"),
    NINE_HEARTS(9, Suit.HEARTS, "9♥"),
    NINE_NO_TRUMP(9, null, "9БК"),

    TEN_SPADES(10, Suit.SPADES, "10♠"),
    TEN_CLUBS(10, Suit.CLUBS, "10♣"),
    TEN_DIAMONDS(10, Suit.DIAMONDS, "10♦"),
    TEN_HEARTS(10, Suit.HEARTS, "10♥"),
    TEN_NO_TRUMP(10, null, "10БК"),

    // Мизер
    MISER(0, null, "Мизер"),

    // Пас
    PASS(-1, null, "Пас");

    private final int tricks;
    private final Suit trumpSuit;
    private final String displayName;

    Contract(int tricks, Suit trumpSuit, String displayName) {
        this.tricks = tricks;
        this.trumpSuit = trumpSuit;
        this.displayName = displayName;
    }

    public int getTricks() {
        return tricks;
    }

    public Suit getTrumpSuit() {
        return trumpSuit;
    }

    public boolean isNoTrump() {
        return trumpSuit == null && tricks > 0;
    }

    public boolean isMiser() {
        return this == MISER;
    }

    public boolean isPass() {
        return this == PASS;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isHigherThan(Contract other) {
        if (this.isPass() || other.isPass()) {
            return false;
        }
        if (this == other) {
            return false;
        }

        // Мизер между 7 и 8
        if (this.isMiser() && !other.isMiser()) {
            return other.tricks < 7 || (other.tricks == 7);
        }
        if (!this.isMiser() && other.isMiser()) {
            return this.tricks >= 8;
        }

        if (this.tricks != other.tricks) {
            return this.tricks > other.tricks;
        }

        // Одинаковое количество взяток - сравниваем масти
        return this.ordinal() > other.ordinal();
    }
}
