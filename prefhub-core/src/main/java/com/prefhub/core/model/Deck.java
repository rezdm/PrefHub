package com.prefhub.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final List<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public List<Card> dealCards(int count) {
        if (count > cards.size()) {
            throw new IllegalStateException("Not enough cards in deck");
        }
        final var dealt = new ArrayList<Card>();
        for (int i = 0; i < count; i++) {
            dealt.add(cards.remove(0));
        }
        return dealt;
    }

    public int size() {
        return cards.size();
    }

    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }
}
