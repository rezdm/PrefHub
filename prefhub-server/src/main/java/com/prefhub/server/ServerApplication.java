package com.prefhub.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.prefhub.server.controllers.AuthController;
import com.prefhub.server.controllers.GameController;
import com.prefhub.server.controllers.RulesController;
import com.prefhub.server.di.ServerModule;
import com.sun.net.httpserver.HttpServer;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.function.Supplier;

public class ServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);
    private final Injector injector;
    private final HttpServer httpServer;

    public ServerApplication(final int port, final String dataDirectory) throws IOException {
        // Initialize Guice injector with module
        this.injector = Guice.createInjector(new ServerModule(dataDirectory));
        logger.info("Services configured");

        // Create Jersey resource config
        final ResourceConfig config = new ResourceConfig();

        // Register resource classes
        config.register(AuthController.class);
        config.register(GameController.class);
        config.register(RulesController.class);
        config.register(com.prefhub.server.web.auth.AuthenticationFilter.class);

        // Register HK2 binder to bind Guice-managed service instances
        config.register(new org.glassfish.hk2.utilities.binding.AbstractBinder() {
            @Override
            protected void configure() {
                bind(injector.getInstance(com.prefhub.server.auth.AuthService.class)).to(com.prefhub.server.auth.AuthService.class);
                bind(injector.getInstance(com.prefhub.server.game.GameService.class)).to(com.prefhub.server.game.GameService.class);
                bind(injector.getInstance(com.prefhub.server.game.RulesLoader.class)).to(com.prefhub.server.game.RulesLoader.class);
                bind(injector.getInstance(com.prefhub.server.web.auth.AuthenticationFilter.class)).to(com.prefhub.server.web.auth.AuthenticationFilter.class);
            }
        });

        // Create HTTP server with Jersey
        final URI baseUri = URI.create("http://localhost:" + port + "/api/");
        this.httpServer = JdkHttpServerFactory.createHttpServer(baseUri, config, false);

        // Add static file handler for root context
        httpServer.createContext("/", new StaticFileHandler());

        logger.info("Server configured on port {}", port);
    }

    public void start() {
        httpServer.start();
        logger.info("Server started");
        logger.info("Web interface available at http://localhost:{}/static/", httpServer.getAddress().getPort());
    }

    public void stop() {
        logger.info("Stopping HTTP server");
        httpServer.stop(0);
        logger.info("HTTP server stopped");
    }

    // Static file handler
    private static class StaticFileHandler implements com.sun.net.httpserver.HttpHandler {
        private static final Logger logger = LoggerFactory.getLogger(StaticFileHandler.class);

        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            // Default to index.html for root
            if (path.isEmpty() || path.equals("/")) {
                path = "/index.html";
            }

            // Try to serve from /static resources
            try (final var is = getClass().getResourceAsStream("/static" + path)) {
                if (is == null) {
                    exchange.sendResponseHeaders(404, 0);
                    exchange.getResponseBody().close();
                    return;
                }

                final byte[] bytes = is.readAllBytes();
                final String contentType = getContentType(path);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                try (final var os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }

        private String getContentType(final String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".json")) return "application/json";
            if (path.endsWith(".ico")) return "image/x-icon";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            return "application/octet-stream";
        }
    }

    public static void main(final String[] args) throws IOException {
        final int port = args.length > 0 ? Integer.parseInt(args[0]) : 8090;
        final String dataDirectory = args.length > 1 ? args[1] : "./game-data";

        final ServerApplication app = new ServerApplication(port, dataDirectory);
        app.start();

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
    }
}
