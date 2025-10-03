package com.prefhub.client.ui;

import com.prefhub.client.api.ApiClient;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ConsoleUI {
    private final ApiClient apiClient;
    private final Scanner scanner;
    private String currentGameId;

    public ConsoleUI(final String serverUrl) {
        this.apiClient = new ApiClient(serverUrl);
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("=== PrefHub Client ===");
        System.out.println("Добро пожаловать в сетевой Преферанс!");
        System.out.println();

        while (true) {
            if (apiClient.getAuthToken() == null) {
                showAuthMenu();
            } else {
                showMainMenu();
            }
        }
    }

    private void showAuthMenu() {
        System.out.println("\n--- Меню аутентификации ---");
        System.out.println("1. Войти");
        System.out.println("2. Зарегистрироваться");
        System.out.println("3. Выход");
        System.out.print("Выберите действие: ");

        final var choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> login();
            case "2" -> register();
            case "3" -> {
                System.out.println("До свидания!");
                System.exit(0);
            }
            default -> System.out.println("Неверный выбор");
        }
    }

    private void showMainMenu() {
        System.out.println("\n--- Главное меню ---");
        System.out.println("1. Создать игру");
        System.out.println("2. Присоединиться к игре");
        System.out.println("3. Список игр");
        System.out.println("4. Состояние игры");
        System.out.println("5. Сделать заявку");
        System.out.println("6. Обменять прикуп");
        System.out.println("7. Сыграть карту");
        System.out.println("8. Следующий раунд");
        System.out.println("9. Выйти из аккаунта");
        System.out.print("Выберите действие: ");

        final var choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> createGame();
            case "2" -> joinGame();
            case "3" -> listGames();
            case "4" -> showGameState();
            case "5" -> placeBid();
            case "6" -> exchangeWidow();
            case "7" -> playCard();
            case "8" -> nextRound();
            case "9" -> logout();
            default -> System.out.println("Неверный выбор");
        }
    }

    private void register() {
        System.out.print("Имя пользователя: ");
        final var username = scanner.nextLine().trim();
        System.out.print("Пароль: ");
        final var password = scanner.nextLine().trim();

        try {
            final var response = apiClient.register(username, password);
            System.out.println("✓ Регистрация успешна: " + response.get("message"));
        } catch (Exception e) {
            System.out.println("✗ Ошибка регистрации: " + e.getMessage());
        }
    }

    private void login() {
        System.out.print("Имя пользователя: ");
        final var username = scanner.nextLine().trim();
        System.out.print("Пароль: ");
        final var password = scanner.nextLine().trim();

        try {
            final var response = apiClient.login(username, password);
            System.out.println("✓ Вход выполнен успешно!");
            System.out.println("Токен: " + response.get("token"));
        } catch (Exception e) {
            System.out.println("✗ Ошибка входа: " + e.getMessage());
        }
    }

    private void logout() {
        try {
            apiClient.logout();
            System.out.println("✓ Выход выполнен успешно");
        } catch (Exception e) {
            System.out.println("✗ Ошибка выхода: " + e.getMessage());
        }
    }

    private void createGame() {
        System.out.print("ID игры: ");
        final var gameId = scanner.nextLine().trim();

        try {
            final var response = apiClient.createGame(gameId);
            this.currentGameId = gameId;
            System.out.println("✓ Игра создана: " + gameId);
            displayGameState(response);
        } catch (Exception e) {
            System.out.println("✗ Ошибка создания игры: " + e.getMessage());
        }
    }

    private void joinGame() {
        System.out.print("ID игры: ");
        final var gameId = scanner.nextLine().trim();

        try {
            final var response = apiClient.joinGame(gameId);
            this.currentGameId = gameId;
            System.out.println("✓ Присоединились к игре: " + gameId);
            displayGameState(response);
        } catch (Exception e) {
            System.out.println("✗ Ошибка присоединения к игре: " + e.getMessage());
        }
    }

    private void listGames() {
        try {
            final var games = apiClient.listGames();
            System.out.println("Список игр:");
            System.out.println(games);
        } catch (Exception e) {
            System.out.println("✗ Ошибка получения списка игр: " + e.getMessage());
        }
    }

    private void showGameState() {
        if (currentGameId == null) {
            System.out.print("ID игры: ");
            currentGameId = scanner.nextLine().trim();
        }

        try {
            final var state = apiClient.getGameState(currentGameId);
            displayGameState(state);
        } catch (Exception e) {
            System.out.println("✗ Ошибка получения состояния игры: " + e.getMessage());
        }
    }

    private void placeBid() {
        if (currentGameId == null) {
            System.out.println("Сначала присоединитесь к игре");
            return;
        }

        System.out.println("Доступные заявки:");
        System.out.println("PASS, SIX_SPADES, SIX_CLUBS, SIX_DIAMONDS, SIX_HEARTS, SIX_NO_TRUMP,");
        System.out.println("SEVEN_SPADES, ..., MISER, и т.д.");
        System.out.print("Ваша заявка: ");
        final var contract = scanner.nextLine().trim();

        try {
            final var response = apiClient.placeBid(currentGameId, contract);
            System.out.println("✓ Заявка сделана: " + response.get("message"));
        } catch (Exception e) {
            System.out.println("✗ Ошибка: " + e.getMessage());
        }
    }

    private void exchangeWidow() {
        if (currentGameId == null) {
            System.out.println("Сначала присоединитесь к игре");
            return;
        }

        System.out.print("Карты для сброса (например: '7♠,8♣'): ");
        final var cards = scanner.nextLine().trim();

        try {
            final var response = apiClient.exchangeWidow(currentGameId, cards);
            System.out.println("✓ Прикуп обменян: " + response.get("message"));
        } catch (Exception e) {
            System.out.println("✗ Ошибка: " + e.getMessage());
        }
    }

    private void playCard() {
        if (currentGameId == null) {
            System.out.println("Сначала присоединитесь к игре");
            return;
        }

        System.out.print("Карта для игры (например: '7♠'): ");
        final var card = scanner.nextLine().trim();

        try {
            final var response = apiClient.playCard(currentGameId, card);
            System.out.println("✓ Карта сыграна: " + response.get("message"));
        } catch (Exception e) {
            System.out.println("✗ Ошибка: " + e.getMessage());
        }
    }

    private void nextRound() {
        if (currentGameId == null) {
            System.out.println("Сначала присоединитесь к игре");
            return;
        }

        try {
            final var response = apiClient.nextRound(currentGameId);
            System.out.println("✓ Следующий раунд начат: " + response.get("message"));
        } catch (Exception e) {
            System.out.println("✗ Ошибка: " + e.getMessage());
        }
    }

    private void displayGameState(final Map<String, Object> state) {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                   СОСТОЯНИЕ ИГРЫ                         ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");

        System.out.println("║ Игра: " + state.get("gameId") + " | Раунд: " + state.get("roundNumber"));
        System.out.println("║ Фаза: " + state.get("phase"));

        // Next action description
        if (state.containsKey("nextActionDescription")) {
            final var nextAction = (String) state.get("nextActionDescription");
            System.out.println("║");
            System.out.println("║ ► " + nextAction);
        }

        // Show if it's your turn
        if (state.containsKey("yourTurn") && (Boolean) state.get("yourTurn")) {
            System.out.println("║ ⚡ ВАШ ХОД! ⚡");
        }

        System.out.println("╠════════════════════════════════════════════════════════════╣");

        // Show hand
        if (state.containsKey("hand")) {
            final var hand = (List<Map<String, String>>) state.get("hand");
            if (hand != null && !hand.isEmpty()) {
                System.out.println("║ ВАШИ КАРТЫ:");
                System.out.print("║   ");
                for (final var card : hand) {
                    System.out.print(formatCard(card) + " ");
                }
                System.out.println();
                System.out.println("╠════════════════════════════════════════════════════════════╣");
            }
        }

        // Show widow (if visible)
        if (state.containsKey("widow") && state.get("widow") != null) {
            final var widow = (List<Map<String, String>>) state.get("widow");
            if (!widow.isEmpty()) {
                System.out.println("║ ПРИКУП:");
                System.out.print("║   ");
                for (final var card : widow) {
                    System.out.print(formatCard(card) + " ");
                }
                System.out.println();
                System.out.println("╠════════════════════════════════════════════════════════════╣");
            }
        }

        // Show contract and trump
        if (state.containsKey("contract") && state.get("contract") != null) {
            final var contract = (Map<String, String>) state.get("contract");
            System.out.println("║ Контракт: " + contract.get("displayName"));
            if (state.containsKey("trumpSuit") && state.get("trumpSuit") != null) {
                final var trump = (Map<String, String>) state.get("trumpSuit");
                System.out.println("║ Козырь: " + trump.get("symbol") + " " + trump.get("russianName"));
            }
        }

        // Show declarer
        if (state.containsKey("declarerUsername") && state.get("declarerUsername") != null) {
            System.out.println("║ Разыгрывающий: " + state.get("declarerUsername"));
        }

        // Show bids
        if (state.containsKey("bids") && state.get("bids") != null) {
            final var bids = (Map<String, String>) state.get("bids");
            if (!bids.isEmpty()) {
                System.out.println("║");
                System.out.println("║ ЗАЯВКИ:");
                for (final var entry : bids.entrySet()) {
                    System.out.println("║   " + entry.getKey() + ": " + entry.getValue());
                }
            }
        }

        // Show current trick
        if (state.containsKey("currentTrick") && state.get("currentTrick") != null) {
            final var trick = (Map<String, Map<String, String>>) state.get("currentTrick");
            if (!trick.isEmpty()) {
                System.out.println("║");
                System.out.println("║ ТЕКУЩАЯ ВЗЯТКА:");
                for (final var entry : trick.entrySet()) {
                    System.out.println("║   " + entry.getKey() + ": " + formatCard(entry.getValue()));
                }
            }
        }

        // Show scores
        if (state.containsKey("scores") && state.get("scores") != null) {
            final var scores = (Map<String, Integer>) state.get("scores");
            System.out.println("║");
            System.out.println("║ СЧЕТ:");
            for (final var entry : scores.entrySet()) {
                var bullets = 0;
                var mountains = 0;
                if (state.containsKey("bullets")) {
                    final var bulletsMap = (Map<String, Integer>) state.get("bullets");
                    bullets = bulletsMap.getOrDefault(entry.getKey(), 0);
                }
                if (state.containsKey("mountains")) {
                    final var mountainsMap = (Map<String, Integer>) state.get("mountains");
                    mountains = mountainsMap.getOrDefault(entry.getKey(), 0);
                }
                System.out.printf("║   %s: %d очков (пуля: %d, гора: %d)%n",
                    entry.getKey(), entry.getValue(), bullets, mountains);
            }
        }

        // Show other players
        if (state.containsKey("otherPlayers")) {
            final var others = (List<String>) state.get("otherPlayers");
            if (!others.isEmpty()) {
                System.out.println("║");
                System.out.println("║ Другие игроки: " + String.join(", ", others));
            }
        }

        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
    }

    private String formatCard(final Map<String, String> card) {
        if (card == null) return "?";
        final var rank = card.get("rank");
        final var suit = card.get("suit");
        if (rank == null || suit == null) return "?";

        // Get display values
        final var rankSymbol = getRankSymbol(rank);
        final var suitSymbol = getSuitSymbol(suit);

        return rankSymbol + suitSymbol;
    }

    private String getRankSymbol(final String rank) {
        return switch (rank) {
            case "SEVEN" -> "7";
            case "EIGHT" -> "8";
            case "NINE" -> "9";
            case "TEN" -> "10";
            case "JACK" -> "В";
            case "QUEEN" -> "Д";
            case "KING" -> "К";
            case "ACE" -> "Т";
            default -> rank;
        };
    }

    private String getSuitSymbol(final String suit) {
        return switch (suit) {
            case "SPADES" -> "♠";
            case "CLUBS" -> "♣";
            case "DIAMONDS" -> "♦";
            case "HEARTS" -> "♥";
            default -> suit;
        };
    }
}
