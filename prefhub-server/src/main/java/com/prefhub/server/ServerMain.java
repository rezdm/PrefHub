package com.prefhub.server;

import com.prefhub.server.scenario.ScenarioRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

            // Check for --scenario argument
            String scenarioFile = null;
            int portArgIndex = 0;
            int storageDirArgIndex = 1;

            for (int i = 0; i < args.length; i++) {
                if ("--scenario".equals(args[i]) && i + 1 < args.length) {
                    scenarioFile = args[i + 1];
                    portArgIndex = i + 2;
                    storageDirArgIndex = i + 3;
                    break;
                }
            }

            if (scenarioFile != null) {
                // Scenario mode - use timestamped test folder
                final var timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
                final var testStorageDir = "./game-data-scenarios/" + timestamp;
                final var port = portArgIndex < args.length ? Integer.parseInt(args[portArgIndex]) : 8090;

                logger.info("Starting PrefHub server in SCENARIO mode...");
                logger.info("Scenario file: {}", scenarioFile);
                logger.info("Port: {}", port);
                logger.info("Test storage directory: {}", testStorageDir);

                final var scenarioRunner = new ScenarioRunner(port, testStorageDir, scenarioFile);
                scenarioRunner.run();

                logger.info("Scenario execution completed. Server is running for testing.");

                // Keep the server running
                Thread.currentThread().join();

            } else {
                // Normal mode
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
            }

        } catch (Exception e) {
            logger.error("Server error: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
