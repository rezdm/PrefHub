package com.prefhub.server;

import com.prefhub.server.api.HttpServer;
import com.prefhub.server.auth.AuthService;
import com.prefhub.server.game.GameService;
import com.prefhub.server.persistence.GamePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ServerMain {
    private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args) {
        try {
            // Prefer IPv4 stack
            System.setProperty("java.net.preferIPv4Stack", "true");

            int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
            String storageDir = args.length > 1 ? args[1] : "./game-data";

            logger.info("Starting PrefHub server...");
            logger.info("Port: {}", port);
            logger.info("Storage directory: {}", storageDir);

            GamePersistence persistence = new GamePersistence(storageDir);
            AuthService authService = new AuthService();
            GameService gameService = new GameService(persistence);

            HttpServer server = new HttpServer(port, authService, gameService);
            server.start();

            logger.info("Server is running. Press Ctrl+C to stop.");

            // Keep the server running
            Thread.currentThread().join();

        } catch (IOException | InterruptedException e) {
            logger.error("Server error: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
