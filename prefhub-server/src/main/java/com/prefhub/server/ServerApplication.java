package com.prefhub.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.prefhub.server.controllers.AuthController;
import com.prefhub.server.controllers.GameController;
import com.prefhub.server.controllers.RulesController;
import com.prefhub.server.di.ServerModule;
import com.prefhub.server.websocket.GameWebSocketServer;
import com.sun.net.httpserver.HttpServer;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

public class ServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);
    private final Injector injector;
    private final HttpServer httpServer;
    private final GameWebSocketServer webSocketServer;

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

        // Register HK2 binder to bind Guice-managed instances
        config.register(new org.glassfish.hk2.utilities.binding.AbstractBinder() {
            @Override
            protected void configure() {
                // Services
                bind(injector.getInstance(com.prefhub.server.auth.AuthService.class)).to(com.prefhub.server.auth.AuthService.class);
                bind(injector.getInstance(com.prefhub.server.game.GameService.class)).to(com.prefhub.server.game.GameService.class);
                bind(injector.getInstance(com.prefhub.server.game.RulesLoader.class)).to(com.prefhub.server.game.RulesLoader.class);
                // Filter
                bind(injector.getInstance(com.prefhub.server.web.auth.AuthenticationFilter.class)).to(com.prefhub.server.web.auth.AuthenticationFilter.class);
                // Controllers
                bindFactory(new org.glassfish.hk2.api.Factory<AuthController>() {
                    @Override
                    public AuthController provide() {
                        return injector.getInstance(AuthController.class);
                    }
                    @Override
                    public void dispose(AuthController instance) {}
                }).to(AuthController.class);
                bindFactory(new org.glassfish.hk2.api.Factory<GameController>() {
                    @Override
                    public GameController provide() {
                        return injector.getInstance(GameController.class);
                    }
                    @Override
                    public void dispose(GameController instance) {}
                }).to(GameController.class);
                bindFactory(new org.glassfish.hk2.api.Factory<RulesController>() {
                    @Override
                    public RulesController provide() {
                        return injector.getInstance(RulesController.class);
                    }
                    @Override
                    public void dispose(RulesController instance) {}
                }).to(RulesController.class);
            }
        });

        // Create HTTP server with Jersey
        final URI baseUri = URI.create("http://localhost:" + port + "/api/");
        this.httpServer = JdkHttpServerFactory.createHttpServer(baseUri, config, false);

        // Add static file handler for root context
        httpServer.createContext("/", new StaticFileHandler());

        // Create WebSocket server on port + 1
        final int wsPort = port + 1;
        this.webSocketServer = new GameWebSocketServer(
            wsPort,
            injector.getInstance(com.prefhub.server.auth.AuthService.class),
            injector.getInstance(com.prefhub.server.game.GameService.class)
        );

        logger.info("Server configured on port {} (HTTP) and {} (WebSocket)", port, wsPort);
    }

    public void start() {
        httpServer.start();
        webSocketServer.start();
        logger.info("Server started");
        logger.info("HTTP API available at http://localhost:{}/api/", httpServer.getAddress().getPort());
        logger.info("WebSocket available at ws://localhost:{}/", webSocketServer.getPort());
        logger.info("Web interface (v0) available at http://localhost:{}/", httpServer.getAddress().getPort());
        logger.info("Web interface (v0) also available at http://localhost:{}/web-v0/", httpServer.getAddress().getPort());
    }

    public void stop() {
        logger.info("Stopping servers");
        try {
            webSocketServer.stop();
        } catch (InterruptedException e) {
            logger.error("Error stopping WebSocket server", e);
        }
        httpServer.stop(0);
        logger.info("Servers stopped");
    }

    // Static file handler
    private static class StaticFileHandler implements com.sun.net.httpserver.HttpHandler {
        private static final Logger logger = LoggerFactory.getLogger(StaticFileHandler.class);
        private static final String DEFAULT_WEB_VERSION = "web-v0";

        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            // Determine which web version to serve
            String resourcePath;

            if (path.isEmpty() || path.equals("/")) {
                // Root path - serve default version's index.html
                resourcePath = "/static/" + DEFAULT_WEB_VERSION + "/index.html";
            } else if (path.startsWith("/web-v")) {
                // Explicit version request (e.g., /web-v0/index.html or /web-v1/style.css)
                // Extract version and file path
                final int secondSlash = path.indexOf('/', 1);
                if (secondSlash == -1) {
                    // Path is just /web-v0 or /web-v1 - serve index.html
                    resourcePath = "/static" + path + "/index.html";
                } else {
                    // Path includes file (e.g., /web-v0/style.css)
                    resourcePath = "/static" + path;
                }
            } else {
                // Other paths - serve from default version
                resourcePath = "/static/" + DEFAULT_WEB_VERSION + path;
            }

            // Try to serve the resource
            try (final var is = getClass().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    logger.debug("Resource not found: {}", resourcePath);
                    exchange.sendResponseHeaders(404, 0);
                    exchange.getResponseBody().close();
                    return;
                }

                final byte[] bytes = is.readAllBytes();
                final String contentType = getContentType(resourcePath);
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
