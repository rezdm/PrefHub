package com.prefhub.server.scenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.prefhub.core.model.*;
import com.prefhub.server.ServerApplication;
import com.prefhub.server.di.ServerModule;
import com.prefhub.server.game.GameService;
import com.prefhub.server.game.RulesLoader;
import com.prefhub.server.repository.GameRepository;
import com.prefhub.server.repository.RulesRepository;
import com.prefhub.server.repository.SessionRepository;
import com.prefhub.server.repository.impl.ResourceRulesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runs test scenarios for end-to-end testing.
 */
public class ScenarioRunner {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioRunner.class);

    private final int port;
    private final String storageDir;
    private final String scenarioFile;

    public ScenarioRunner(final int port, final String storageDir, final String scenarioFile) {
        this.port = port;
        this.storageDir = storageDir;
        this.scenarioFile = scenarioFile;
    }

    public void run() throws Exception {
        logger.info("Running scenario: {}", scenarioFile);

        // Load test scenario from resources
        final TestScenario scenario = loadScenario();

        // Create resource-based rules repository for test mode
        final var resourceRulesRepo = new ResourceRulesRepository("/rules");

        // Create custom injector with resource-based rules repository
        // Use Modules.override to replace the FileRulesRepository with ResourceRulesRepository
        final var baseModule = new ServerModule(storageDir);
        final var overrideModule = new com.google.inject.AbstractModule() {
            @Override
            protected void configure() {
                bind(RulesRepository.class).toInstance(resourceRulesRepo);
            }
        };
        final var injector = Guice.createInjector(com.google.inject.util.Modules.override(baseModule).with(overrideModule));

        final var rulesLoader = new RulesLoader(injector.getInstance(RulesRepository.class));
        final var gameRepository = injector.getInstance(GameRepository.class);
        final var sessionRepository = injector.getInstance(SessionRepository.class);
        final var gameService = new GameService(gameRepository, rulesLoader);

        // Create test players with fixed session tokens for easy testing
        final String playerWestId = "test-token-west-" + UUID.randomUUID().toString().substring(0, 8);
        final String playerEastId = "test-token-east-" + UUID.randomUUID().toString().substring(0, 8);
        final String playerSouthId = "test-token-south-" + UUID.randomUUID().toString().substring(0, 8);

        sessionRepository.save(playerWestId, "playerWest");
        sessionRepository.save(playerEastId, "playerEast");
        sessionRepository.save(playerSouthId, "playerSouth");

        logger.info("Created test players with session tokens:");
        logger.info("  playerWest:  {}", playerWestId);
        logger.info("  playerEast:  {}", playerEastId);
        logger.info("  playerSouth: {}", playerSouthId);

        // Write tokens to a file for the Python client to use
        try {
            final var tokensFile = new java.io.File(storageDir, "test-tokens.json");
            final var tokensJson = String.format(
                "{\"playerWest\":\"%s\",\"playerEast\":\"%s\",\"playerSouth\":\"%s\"}",
                playerWestId, playerEastId, playerSouthId
            );
            java.nio.file.Files.writeString(tokensFile.toPath(), tokensJson);
            logger.info("Wrote session tokens to: {}", tokensFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to write tokens file", e);
        }

        // Process each test case
        for (final var entry : scenario.getTests().entrySet()) {
            final var testId = entry.getKey();
            final var testCase = entry.getValue();

            logger.info("Setting up test: {} - {}", testId, testCase.getDescription());

            // Create game with sochinka rules (default for test)
            final var gameId = testCase.getGame();
            final var gameState = gameService.createGame(gameId, "sochinka");

            // Add players in order: South, West, East (so South is dealer at index 0)
            final var playerSouth = new Player("playerSouth");
            final var playerWest = new Player("playerWest");
            final var playerEast = new Player("playerEast");

            gameState.addPlayer(playerSouth);
            gameState.addPlayer(playerWest);
            gameState.addPlayer(playerEast);

            // Set up custom deck
            setupCustomDeck(gameState, testCase.getDeck(), playerWest, playerEast, playerSouth);

            // Set dealer to playerSouth (index 0)
            gameState.setDealerIndex(0);
            gameState.setCurrentPlayerIndex(0);
            gameState.setPhase(GamePhase.BIDDING);

            gameRepository.save(gameState);

            logger.info("Test {} setup complete. Game ID: {}, Dealer: playerSouth",
                testId, gameId);
        }

        // Start the server
        logger.info("Starting server for scenario testing...");
        final var app = new ServerApplication(port, storageDir);
        app.start();
    }

    private TestScenario loadScenario() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        try (final InputStream inputStream = getClass().getResourceAsStream("/" + scenarioFile)) {
            if (inputStream == null) {
                throw new IOException("Scenario file not found: " + scenarioFile);
            }
            return mapper.readValue(inputStream, TestScenario.class);
        }
    }

    private void setupCustomDeck(final GameState gameState,
                                  final TestScenario.DeckSetup deckSetup,
                                  final Player playerWest,
                                  final Player playerEast,
                                  final Player playerSouth) {
        logger.info("Setting up custom deck configuration");

        // Parse and assign cards to each player
        final List<Card> westCards = parseCards(deckSetup.getPlayerWest());
        final List<Card> eastCards = parseCards(deckSetup.getPlayerEast());
        final List<Card> southCards = parseCards(deckSetup.getPlayerSouth());
        final List<Card> prikupCards = parseCards(deckSetup.getPrikup());

        // Assign hands
        playerWest.setHand(westCards);
        playerEast.setHand(eastCards);
        playerSouth.setHand(southCards);

        // Set widow (prikup)
        gameState.setWidow(prikupCards);

        logger.info("Custom deck setup: West={} cards, East={} cards, South={} cards, Prikup={} cards",
            westCards.size(), eastCards.size(), southCards.size(), prikupCards.size());
    }

    private List<Card> parseCards(final List<String> cardStrings) {
        final List<Card> cards = new ArrayList<>();
        for (final var cardString : cardStrings) {
            cards.add(parseCard(cardString));
        }
        return cards;
    }

    private Card parseCard(final String cardString) {
        // Expected format: RANK_SUIT (e.g., "ACE_SPADES")
        final String[] parts = cardString.split("_");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid card format: " + cardString);
        }

        final Rank rank = Rank.valueOf(parts[0]);
        final Suit suit = Suit.valueOf(parts[1]);

        return new Card(suit, rank);
    }
}
