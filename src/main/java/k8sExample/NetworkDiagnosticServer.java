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
        System.out.println("Starting server on port " + config.getPort());
        configureServer();
        setupRoutes();
        setupErrorHandling();
        System.out.println("Server started successfully");
    }

    private void configureServer() {
        port(config.getPort());
        threadPool(8, 2, 30000);
        
        // 정적 파일 설정 수정
        staticFiles.location("/public");
        staticFiles.expireTime(600);
        
        // before 필터 수정
        before((request, response) -> {
            String path = request.pathInfo();
            // 정적 파일 요청이나 이미 /checkutil로 시작하는 경우는 리다이렉트하지 않음
            if (!path.startsWith("/checkutil") && 
                !path.startsWith("/public") && 
                !path.startsWith("/css") && 
                !path.startsWith("/js")) {
                response.redirect("/checkutil" + path);
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
        path("/checkutil", () -> {
            get("/", requestHandler::handleHome);
            get("", requestHandler::handleHome);  // 슬래시 없는 경우도 처리
            get("/health", requestHandler::handleHealthCheck);
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