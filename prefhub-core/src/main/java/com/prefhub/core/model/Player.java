package com.prefhub.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Player implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final List<Card> hand;
    private int score;
    private int bullet; // пуля
    private int mountain; // гора

    public Player(String username) {
        this.username = username;
        this.hand = new ArrayList<>();
        this.score = 0;
        this.bullet = 0;
        this.mountain = 0;
    }

    public String getUsername() {
        return username;
    }

    public List<Card> getHand() {
        return new ArrayList<>(hand);
    }

    public void addCard(Card card) {
        hand.add(card);
    }

    public void addCards(List<Card> cards) {
        hand.addAll(cards);
    }

    public Card removeCard(Card card) {
        if (!hand.remove(card)) {
            throw new IllegalArgumentException("Card not in hand: " + card);
        }
        return card;
    }

    public void clearHand() {
        hand.clear();
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void addScore(int points) {
        this.score += points;
    }

    public int getBullet() {
        return bullet;
    }

    public void setBullet(int bullet) {
        this.bullet = bullet;
    }

    public void addBullet(int amount) {
        this.bullet += amount;
    }

    public int getMountain() {
        return mountain;
    }

    public void setMountain(int mountain) {
        this.mountain = mountain;
    }

    public void addMountain(int amount) {
        this.mountain += amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(username, player.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return username;
    }
}
