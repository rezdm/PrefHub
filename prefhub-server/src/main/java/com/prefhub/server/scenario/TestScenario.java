package com.prefhub.server.scenario;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class TestScenario {
    @JsonProperty("tests")
    private Map<String, TestCase> tests;

    public TestScenario() {
    }

    public Map<String, TestCase> getTests() {
        return tests;
    }

    public void setTests(final Map<String, TestCase> tests) {
        this.tests = tests;
    }

    public static class TestCase {
        @JsonProperty("description")
        private String description;

        @JsonProperty("game")
        private String game;

        @JsonProperty("deck")
        private DeckSetup deck;

        @JsonProperty("scenarios")
        private List<Scenario> scenarios;

        public TestCase() {
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public String getGame() {
            return game;
        }

        public void setGame(final String game) {
            this.game = game;
        }

        public DeckSetup getDeck() {
            return deck;
        }

        public void setDeck(final DeckSetup deck) {
            this.deck = deck;
        }

        public List<Scenario> getScenarios() {
            return scenarios;
        }

        public void setScenarios(final List<Scenario> scenarios) {
            this.scenarios = scenarios;
        }
    }

    public static class Scenario {
        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("moves")
        private List<Move> moves;

        public Scenario() {
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public List<Move> getMoves() {
            return moves;
        }

        public void setMoves(final List<Move> moves) {
            this.moves = moves;
        }
    }

    public static class Move {
        @JsonProperty("action")
        private String action;

        @JsonProperty("player")
        private String player;

        @JsonProperty("contract")
        private String contract;

        @JsonProperty("discard")
        private List<String> discard;

        @JsonProperty("card")
        private String card;

        public Move() {
        }

        public String getAction() {
            return action;
        }

        public void setAction(final String action) {
            this.action = action;
        }

        public String getPlayer() {
            return player;
        }

        public void setPlayer(final String player) {
            this.player = player;
        }

        public String getContract() {
            return contract;
        }

        public void setContract(final String contract) {
            this.contract = contract;
        }

        public List<String> getDiscard() {
            return discard;
        }

        public void setDiscard(final List<String> discard) {
            this.discard = discard;
        }

        public String getCard() {
            return card;
        }

        public void setCard(final String card) {
            this.card = card;
        }
    }

    public static class DeckSetup {
        @JsonProperty("playerWest")
        private List<String> playerWest;

        @JsonProperty("playerEast")
        private List<String> playerEast;

        @JsonProperty("playerSouth")
        private List<String> playerSouth;

        @JsonProperty("prikup")
        private List<String> prikup;

        public DeckSetup() {
        }

        public List<String> getPlayerWest() {
            return playerWest;
        }

        public void setPlayerWest(final List<String> playerWest) {
            this.playerWest = playerWest;
        }

        public List<String> getPlayerEast() {
            return playerEast;
        }

        public void setPlayerEast(final List<String> playerEast) {
            this.playerEast = playerEast;
        }

        public List<String> getPlayerSouth() {
            return playerSouth;
        }

        public void setPlayerSouth(final List<String> playerSouth) {
            this.playerSouth = playerSouth;
        }

        public List<String> getPrikup() {
            return prikup;
        }

        public void setPrikup(final List<String> prikup) {
            this.prikup = prikup;
        }
    }
}
