package k8sExample;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/checkutil")
public class NetworkDiagnosticServer {
    private final ServerConfig config;
    private final NetworkCommandExecutor commandExecutor;
    private final RequestHandler requestHandler;

    @Autowired
    public NetworkDiagnosticServer(
        ServerConfig config,
        NetworkCommandExecutor commandExecutor,
        RequestHandler requestHandler
    ) {
        this.config = config;
        this.commandExecutor = commandExecutor;
        this.requestHandler = requestHandler;
    }

    @GetMapping({"", "/"})
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView("index");
        try {
            modelAndView.addObject("serverInfo", requestHandler.generateServerInfo());
        } catch (Exception e) {
            modelAndView.addObject("error", "서버 정보를 가져오는데 실패했습니다.");
        }
        return modelAndView;
    }

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"status\":\"OK\"}");
    }

    @PostMapping("/netcat")
    @ResponseBody
    public ResponseEntity<String> netcat(@RequestBody NetcatRequest request) {
        try {
            String command = String.format("nc -zv %s %s", request.getIp(), request.getPort());
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
    public ResponseEntity<String> nslookup(@RequestBody DnsRequest request) {
        try {
            String command = String.format("nslookup %s", request.getUrl());
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
    public ResponseEntity<String> curl(@RequestBody UrlRequest request) {
        try {
            String command = String.format("curl -v %s", request.getUrl());
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(commandExecutor.execute(command));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("{\"error\":\"Invalid request parameters\"}");
        }
    }
}

// Request DTO classes
class NetcatRequest {
    private String ip;
    private String port;
    
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getPort() { return port; }
    public void setPort(String port) { this.port = port; }
}

class DnsRequest {
    private String url;
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}

class UrlRequest {
    private String url;
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
} 