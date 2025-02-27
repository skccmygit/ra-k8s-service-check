package k8sExample;

import static spark.Spark.*;
import java.time.LocalDateTime;
import javax.servlet.http.HttpServletResponse;

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
        
        // 정적 파일의 경로를 /checkutil 아래로 설정
        staticFiles.location("/public");
        staticFiles.header("X-Content-Type-Options", "nosniff");
        
        // 모든 요청에 대해 /checkutil 프리픽스 추가
        before((request, response) -> {
            if (!request.pathInfo().startsWith("/checkutil")) {
                response.redirect("/checkutil" + request.pathInfo());
            }
        });
        
        // Set response timeout using proper Spark/Servlet methods
        before((request, response) -> {
            if (response.raw() instanceof HttpServletResponse) {
                HttpServletResponse raw = response.raw();
                raw.setHeader("Connection", "keep-alive");
                raw.setHeader("Keep-Alive", "timeout=10");
            }
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
        // 모든 라우트에 /checkutil 프리픽스 추가
        path("/checkutil", () -> {
            get("/health", requestHandler::handleHealthCheck);
            get("/", requestHandler::handleHome);
            post("/netcat", requestHandler::handleNetcat);
            post("/nslookup", requestHandler::handleNslookup);
            post("/curl", requestHandler::handleCurl);
            post("/message", requestHandler::handleMessage);
        });
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