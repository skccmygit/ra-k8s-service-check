package k8sExample;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class NetworkDiagnosticServer {
    private static final Logger logger = LoggerFactory.getLogger(NetworkDiagnosticServer.class);

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

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<String> healthCheck() {
        logger.info("헬스 체크 엔드포인트 호출됨");
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"status\":\"OK\"}");
    }

    @GetMapping({"/", "/index"})
    public ModelAndView index() {
        logger.info("Index 엔드포인트 호출됨");
        ModelAndView modelAndView = new ModelAndView("index");
        try {
            String serverInfo = requestHandler.generateServerInfo();
            logger.info("서버 정보 생성 성공: {}", serverInfo);
            modelAndView.addObject("serverInfo", serverInfo);
        } catch (Exception e) {
            logger.error("서버 정보 생성 중 오류 발생", e);
            modelAndView.addObject("serverInfo", "서버 정보를 가져오는데 실패했습니다: " + e.getMessage());
        }
        return modelAndView;
    }

    @PostMapping("/netcat")
    @ResponseBody
    public ResponseEntity<String> netcat(@RequestBody NetcatRequest request) {
        logger.debug("Netcat request received for IP: {} and Port: {}", request.getIp(), request.getPort());
        try {
            String command = String.format("nc -zv %s %s", request.getIp(), request.getPort());
            String result = commandExecutor.execute(command);
            logger.debug("Netcat command executed successfully: {}", result);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
        } catch (Exception e) {
            logger.error("Error executing netcat command", e);
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
            String url = request.getUrl();
            
            // URL에 http:// 또는 https:// 접두사가 없으면 http:// 추가
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            
            String command = String.format("curl -v %s", url);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(commandExecutor.execute(command));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("{\"error\":\"Invalid request parameters\"}");
        }
    }

    // Request DTO classes
    static class NetcatRequest {
        private String ip;
        private String port;
        
        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }
        public String getPort() { return port; }
        public void setPort(String port) { this.port = port; }
    }

    static class DnsRequest {
        private String url;
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    static class UrlRequest {
        private String url;
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    } 

}