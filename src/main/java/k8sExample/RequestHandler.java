package k8sExample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class RequestHandler {
    private final NetworkCommandExecutor commandExecutor;
    private final ServerConfig config;

    @Autowired
    public RequestHandler(NetworkCommandExecutor commandExecutor, ServerConfig config) {
        this.commandExecutor = commandExecutor;
        this.config = config;
    }

    @PostMapping("/netcat")
    @ResponseBody
    public ResponseEntity<String> handleNetcat(@RequestBody Map<String, String> params) {
        try {
            String ip = params.get("ip");
            String port = params.get("port");
            String command = String.format("nc -zv %s %s", ip, port);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(commandExecutor.execute(command));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("{\"error\":\"Invalid request parameters\"}");
        }
    }

    @PostMapping("/nslookup")
    @ResponseBody
    public ResponseEntity<String> handleNslookup(@RequestBody Map<String, String> params) {
        try {
            String url = params.get("url");
            String command = String.format("nslookup %s", url);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(commandExecutor.execute(command));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("{\"error\":\"Invalid request parameters\"}");
        }
    }

    @PostMapping("/curl")
    @ResponseBody
    public ResponseEntity<String> handleCurl(@RequestBody Map<String, String> params) {
        try {
            String url = params.get("url");
            String command = String.format("curl -v %s", url);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(commandExecutor.execute(command));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("{\"error\":\"Invalid request parameters\"}");
        }
    }

    public String generateServerInfo() throws Exception {
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
} 