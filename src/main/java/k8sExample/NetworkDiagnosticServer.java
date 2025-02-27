package k8sExample;

import static spark.Spark.*;
import java.time.LocalDateTime;

public class NetworkDiagnosticServer {
    private final ServerConfig config;
    private final NetworkCommandExecutor commandExecutor;
    private final RequestHandler requestHandler;

    public NetworkDiagnosticServer() {
        this.config = new ServerConfig();
        this.commandExecutor = new NetworkCommandExecutor();
        this.requestHandler = new RequestHandler(commandExecutor);
    }

    public void start() {
        configureServer();
        setupRoutes();
        setupErrorHandling();
    }

    private void configureServer() {
        port(config.getPort());
        threadPool(8, 2, 30000);
        staticFiles.location("/public");
        
        // Set timeout to 10 seconds
        before((request, response) -> {
            response.raw().setTimeoutHeader(10 * 1000); // 10 seconds in milliseconds
        });
        
        enableCORS();
        configureCompression();
        setupGracefulShutdown();
    }

    private void enableCORS() {
        before((req, res) -> { 
            res.header("Access-Control-Allow-Origin", "*"); 
            res.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS"); 
            res.header("Access-Control-Allow-Headers", "*"); 
        });

        options("/*", (req, res) -> {
            res.status(200);
            return "OK";
        });
    }

    private void configureCompression() {
        after((req, res) -> {
            res.header("Content-Encoding", "gzip");
        });
    }

    private void setupGracefulShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping server...");
            stop();
            System.out.println("Server stopped");
        }));
    }

    private void setupRoutes() {
        before((req, res) -> {
            System.out.printf("Request: %s %s | Headers: %s | QueryParams: %s%n",
                req.requestMethod(),
                req.pathInfo(),
                req.headers(),
                req.queryParams()
            );
        });

        get("/health", requestHandler::handleHealthCheck);
        get("/", requestHandler::handleHome);
        post("/netcat", requestHandler::handleNetcat);
        post("/nslookup", requestHandler::handleNslookup);
        post("/curl", requestHandler::handleCurl);
        post("/message", requestHandler::handleMessage);
    }

    private void setupErrorHandling() {
        exception(Exception.class, (e, req, res) -> {
            e.printStackTrace();
            res.status(500);
            res.type("application/json");
            res.body("{\"error\":\"Internal server error\"}");
        });
    }
} 