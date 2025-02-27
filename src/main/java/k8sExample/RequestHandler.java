package k8sExample;

import spark.Request;
import spark.Response;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RequestHandler {
    private final NetworkCommandExecutor commandExecutor;
    private final ServerConfig config;
    private final String htmlTemplate;

    public RequestHandler(NetworkCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        this.config = new ServerConfig();
        this.htmlTemplate = loadHtmlTemplate();
    }

    public Object handleHealthCheck(Request req, Response res) {
        res.type("application/json");
        return "{\"status\":\"UP\",\"version\":\"" + config.getVersion() + "\"}";
    }

    public Object handleHome(Request req, Response res) {
        try {
            System.out.println("Handling home request: " + req.pathInfo());
            String serverInfo = generateServerInfo();
            String result = String.format(htmlTemplate, serverInfo);
            res.type("text/html");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            res.status(500);
            return "Error getting server information";
        }
    }

    public Object handleNetcat(Request req, Response res) {
        res.type("application/json");
        String ip = req.queryParams("ip");
        String port = req.queryParams("port");
        String command = String.format("nc -zv %s %s", ip, port);
        return commandExecutor.execute(command);
    }

    public Object handleNslookup(Request req, Response res) {
        res.type("application/json");
        String url = req.queryParams("url");
        String command = String.format("nslookup %s", url);
        return commandExecutor.execute(command);
    }

    public Object handleCurl(Request req, Response res) {
        res.type("application/json");
        String url = req.queryParams("url");
        String command = String.format("curl -v %s", url);
        return commandExecutor.execute(command);
    }

    public Object handleMessage(Request req, Response res) {
        try {
            String body = req.body();
            res.type("application/json; charset=UTF-8");
            return String.format(
                "{\"message\":\"받은데이터: %s\",\"timestamp\":\"%s\"}",
                body,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
        } catch (Exception e) {
            e.printStackTrace();
            res.status(500);
            return "{\"error\":\"Failed to process request\"}";
        }
    }

    private String generateServerInfo() throws Exception {
        InetAddress ip = InetAddress.getLocalHost();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        return String.format(
            "Server IP Address: %s <br>" +
            "Version: %s <br>" +
            "App Start Time: %s <br>" +
            "Cluster Environment: %s <br>" +
            "Cluster Name: %s",
            ip.getHostAddress(),
            config.getVersion(),
            config.getStartTime().format(formatter),
            config.getClusterEnv(),
            config.getClusterName()
        );
    }

    private String loadHtmlTemplate() {
        try {
            System.out.println("Loading HTML template"); // 디버깅용 로그
            var inputStream = getClass().getResourceAsStream("/templates/index.html");
            if (inputStream == null) {
                throw new RuntimeException("Template file not found");
            }
            String template = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("Template loaded successfully"); // 디버깅용 로그
            return template;
        } catch (IOException e) {
            System.err.println("Failed to load template: " + e.getMessage()); // 디버깅용 로그
            e.printStackTrace();
            throw new RuntimeException("Failed to load HTML template", e);
        }
    }
} 