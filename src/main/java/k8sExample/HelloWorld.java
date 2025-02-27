package k8sExample;
import static spark.Spark.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class HelloWorld {
    //App 시작시간
    private static final LocalDateTime startTime = LocalDateTime.now();
    private static final String VERSION = "1.2";

    public static void main(String[] args) {
        //시작 시간 출력
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println("App start time: " + startTime.format(formatter));

        // Enhanced request logging
        before((req, res) -> {
            System.out.printf("Request: %s %s | Headers: %s | QueryParams: %s%n",
                req.requestMethod(),
                req.pathInfo(),
                req.headers(),
                req.queryParams()
            );
        });

        // CORS 설정 
        before((req, res) -> { 
            res.header("Access-Control-Allow-Origin", "*"); 
            res.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS"); 
            res.header("Access-Control-Allow-Headers", "*"); 
        });

        // Health check endpoint
        get("/health", (req, res) -> {
            res.type("application/json");
            return "{\"status\":\"UP\",\"version\":\"" + VERSION + "\"}";
        });

        get("/", (req, res) -> {
            InetAddress ip = null;
            String clusterEnv = System.getenv().getOrDefault("CLUSTER_ENV", "unknown");
            String clusterName = System.getenv().getOrDefault("CLUSTER_NAME", "unknown");
            
            try {
                ip = InetAddress.getLocalHost();
                return String.format(
                    "Hello World! <br>" +
                    "Server IP Address: %s <br>" +
                    "Version: %s <br>" +
                    "App Start Time: %s <br>" +
                    "Cluster Environment: %s <br>" +
                    "Cluster Name: %s",
                    ip.getHostAddress(),
                    VERSION,
                    startTime.format(formatter),
                    clusterEnv,
                    clusterName
                );
            } catch (UnknownHostException e) {
                e.printStackTrace();
                res.status(500);
                return "Error getting server information";
            }
        });

        // Enhanced POST endpoint with error handling
        post("/message", (req, res) -> {
            try {
                System.out.println("Processing POST request to: " + req.pathInfo());
                String body = req.body();
                res.type("application/json; charset=UTF-8");
                return String.format(
                    "{\"message\":\"받은데이터: %s\",\"timestamp\":\"%s\"}",
                    body,
                    LocalDateTime.now().format(formatter)
                );
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "{\"error\":\"Failed to process request\"}";
            }
        });

        // Error handling for exceptions
        exception(Exception.class, (e, req, res) -> {
            e.printStackTrace();
            res.status(500);
            res.type("application/json");
            res.body("{\"error\":\"Internal server error\"}");
        });
    }
}
