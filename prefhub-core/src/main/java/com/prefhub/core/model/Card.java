package com.prefhub.core.model;

public record Card(Suit suit, Rank rank) implements java.io.Serializable {
    @Override
    public String toString() {
        return rank.getSymbol() + suit.getSymbol();
    }
}
