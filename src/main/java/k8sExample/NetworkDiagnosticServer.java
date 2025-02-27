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
        staticFiles.location("/public");  // 클래스패스 내의 public 디렉토리 사용
        staticFiles.header("Access-Control-Allow-Origin", "*");
        staticFiles.expireTime(600);
        
        // context path 설정
        before((request, response) -> {
            String path = request.pathInfo();
            System.out.println("Incoming request path: " + path);
        });
        
        // Set response timeout
        before((request, response) -> {
            if (response.raw() instanceof HttpServletResponse) {
                HttpServletResponse raw = response.raw();
                raw.setHeader("Connection", "keep-alive");
                raw.setHeader("Keep-Alive", "timeout=60");
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
        get("/checkutil", (req, res) -> {
            System.out.println("Handling /checkutil path");
            return requestHandler.handleHome(req, res);
        });
        
        get("/checkutil/", (req, res) -> {
            System.out.println("Handling /checkutil/ path");
            return requestHandler.handleHome(req, res);
        });
        
        // 정적 리소스 경로 추가
        get("/checkutil/css/*", (req, res) -> {
            res.type("text/css");
            return null;
        });
        
        get("/checkutil/js/*", (req, res) -> {
            res.type("application/javascript");
            return null;
        });
        
        get("/checkutil/health", requestHandler::handleHealthCheck);
        post("/checkutil/netcat", requestHandler::handleNetcat);
        post("/checkutil/nslookup", requestHandler::handleNslookup);
        post("/checkutil/curl", requestHandler::handleCurl);
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