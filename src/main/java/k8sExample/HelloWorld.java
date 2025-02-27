package k8sExample;

import static spark.Spark.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

public class HelloWorld {
    private static final String VERSION = "1.2";
    private static final LocalDateTime START_TIME = LocalDateTime.now();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String TERMINAL_HTML_TEMPLATE = 
        "<!DOCTYPE html>" +
        "<html>" +
        "<head>" +
        "    <title>K8s Service Check</title>" +
        "    <link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/xterm@4.19.0/css/xterm.css'>" +
        "    <script src='https://cdn.jsdelivr.net/npm/xterm@4.19.0/lib/xterm.js'></script>" +
        "</head>" +
        "<body>" +
        "    <div>" +
        "        Hello World! <br>" +
        "        Server IP Address: %s <br>" +
        "        Version: %s <br>" +
        "        App Start Time: %s <br>" +
        "        Cluster Environment: %s <br>" +
        "        Cluster Name: %s" +
        "    </div>" +
        "    <div id='terminal' style='height: 400px;'></div>" +
        "    <script>" +
        "        var term = new Terminal();" +
        "        term.open(document.getElementById('terminal'));" +
        "        var protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';" +
        "        var ws = new WebSocket(protocol + '//' + window.location.host + '/terminal');" +
        "        ws.onmessage = function(event) { term.write(event.data); };" +
        "        term.onData(function(data) { ws.send(data); });" +
        "    </script>" +
        "</body>" +
        "</html>";

    public static void main(String[] args) {
        initializeServer();
        setupRoutes();
        setupErrorHandling();
    }

    private static void initializeServer() {
        System.out.println("App start time: " + START_TIME.format(DATE_FORMATTER));
        setupRequestLogging();
        setupCORS();
    }

    private static void setupRequestLogging() {
        before((req, res) -> {
            System.out.printf("Request: %s %s | Headers: %s | QueryParams: %s%n",
                req.requestMethod(),
                req.pathInfo(),
                req.headers(),
                req.queryParams()
            );
        });
    }

    private static void setupCORS() {
        before((req, res) -> { 
            res.header("Access-Control-Allow-Origin", "*"); 
            res.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS"); 
            res.header("Access-Control-Allow-Headers", "*"); 
        });
    }

    private static void setupRoutes() {
        setupHealthCheckEndpoint();
        setupMainEndpoint();
        setupWebSocketEndpoint();
        setupMessageEndpoint();
    }

    private static void setupHealthCheckEndpoint() {
        get("/health", (req, res) -> {
            res.type("application/json");
            return String.format("{\"status\":\"UP\",\"version\":\"%s\"}", VERSION);
        });
    }

    private static void setupMainEndpoint() {
        get("/", (req, res) -> generateMainPageHtml());
    }

    private static String generateMainPageHtml() throws UnknownHostException {
        InetAddress ip = InetAddress.getLocalHost();
        String clusterEnv = System.getenv().getOrDefault("CLUSTER_ENV", "unknown");
        String clusterName = System.getenv().getOrDefault("CLUSTER_NAME", "unknown");
        
        return String.format(TERMINAL_HTML_TEMPLATE,
            ip.getHostAddress(),
            VERSION,
            START_TIME.format(DATE_FORMATTER),
            clusterEnv,
            clusterName
        );
    }

    private static void setupWebSocketEndpoint() {
        webSocket("/terminal", TerminalWebSocket.class);
    }

    private static void setupMessageEndpoint() {
        post("/message", (req, res) -> {
            String body = req.body();
            res.type("application/json; charset=UTF-8");
            return String.format(
                "{\"message\":\"받은데이터: %s\",\"timestamp\":\"%s\"}",
                body,
                LocalDateTime.now().format(DATE_FORMATTER)
            );
        });
    }

    private static void setupErrorHandling() {
        exception(Exception.class, (e, req, res) -> {
            e.printStackTrace();
            res.status(500);
            res.type("application/json");
            res.body("{\"error\":\"Internal server error\"}");
        });
    }

    @WebSocket
    public static class TerminalWebSocket {
        private Process process;
        private boolean isConnected = false;
        private static final int UNAUTHORIZED = 4001;
        private static final int TERMINAL_START_FAILED = 4000;
        private static final int MESSAGE_PROCESSING_FAILED = 4002;
        
        @OnWebSocketConnect
        public void onConnect(Session session) throws Exception {
            if (!isAllowedOrigin(session)) {
                handleError(session, null, UNAUTHORIZED, "Unauthorized origin");
                return;
            }
            
            try {
                initializeTerminalProcess(session);
            } catch (Exception e) {
                handleError(session, e, TERMINAL_START_FAILED, "Failed to start terminal");
            }
        }

        private void initializeTerminalProcess(Session session) throws IOException {
            process = new ProcessBuilder("/bin/bash")
                .redirectErrorStream(true)
                .start();
            isConnected = true;
            
            new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = process.getInputStream().read(buffer)) != -1) {
                        session.getRemote().sendString(new String(buffer, 0, len));
                    }
                } catch (IOException e) {
                    handleError(session, e, MESSAGE_PROCESSING_FAILED, "Failed to process stream");
                }
            }).start();
        }
        
        @OnWebSocketClose
        public void onClose(Session session, int statusCode, String reason) {
            cleanupProcess();
        }
        
        @OnWebSocketMessage
        public void onMessage(Session session, String message) {
            if (!isValidSession()) {
                return;
            }
            
            try {
                process.getOutputStream().write(message.getBytes());
                process.getOutputStream().flush();
            } catch (IOException e) {
                handleError(session, e, MESSAGE_PROCESSING_FAILED, "Failed to process message");
            }
        }

        private boolean isValidSession() {
            return isConnected && process != null;
        }

        private void cleanupProcess() {
            if (process != null) {
                process.destroy();
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
            isConnected = false;
        }

        private void handleError(Session session, Exception e, int code, String message) {
            if (e != null) {
                e.printStackTrace();
            }
            if (session.isOpen()) {
                try {
                    session.close(code, message);
                } catch (Exception ignored) {
                    // WebSocket spec에 따라 close()는 IOException 외에도 
                    // IllegalStateException 등을 발생시킬 수 있음
                }
            }
        }
        
        private boolean isAllowedOrigin(Session session) {
            return true; // TODO: Implement proper origin checking logic
        }
    }
}
