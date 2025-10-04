package com.prefhub.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for PrefHub server.
 * Now uses the new DI-based architecture.
 */
public class ServerMain {
    private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args) {
        try {
            // Prefer IPv4 stack
            System.setProperty("java.net.preferIPv4Stack", "true");

            final var port = args.length > 0 ? Integer.parseInt(args[0]) : 8090;
            final var storageDir = args.length > 1 ? args[1] : "./game-data";

            logger.info("Starting PrefHub server...");
            logger.info("Port: {}", port);
            logger.info("Storage directory: {}", storageDir);

            final var app = new ServerApplication(port, storageDir);
            app.start();

            logger.info("Server is running. Press Ctrl+C to stop.");

            // Keep the server running
            Thread.currentThread().join();

        } catch (Exception e) {
            logger.error("Server error: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
