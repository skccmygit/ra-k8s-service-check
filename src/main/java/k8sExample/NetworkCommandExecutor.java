package k8sExample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.io.File;
import org.springframework.stereotype.Service;

@Service
public class NetworkCommandExecutor {
    private static final Logger logger = LoggerFactory.getLogger(NetworkCommandExecutor.class);

    public String execute(String command) {
        if (!isValidCommand(command)) {
            return formatOutput("Invalid command");
        }

        try {
            ProcessBuilder processBuilder = createProcessBuilder(command);
            Process process = processBuilder.start();
            
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return formatOutput("Command timed out after 10 seconds");
            }

            String output = readProcessOutput(process);
            if (process.exitValue() != 0) {
                return formatOutput("Command failed: " + output);
            }

            return formatOutput(output);
        } catch (Exception e) {
            logger.error("Failed to execute command: " + command, e);
            return formatOutput("Error executing command: " + e.getMessage());
        }
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        String charset = StandardCharsets.UTF_8.name();
        
        try (BufferedReader reader = new BufferedReader(
                 new InputStreamReader(process.getInputStream(), charset), 8192);
             BufferedReader errorReader = new BufferedReader(
                 new InputStreamReader(process.getErrorStream(), charset), 8192)) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (output.length() > 100_000) { // Limit output size
                    output.append("... (output truncated)");
                    break;
                }
            }
            while ((line = errorReader.readLine()) != null) {
                output.append("ERROR: ").append(line).append("\n");
                if (output.length() > 100_000) {
                    output.append("... (output truncated)");
                    break;
                }
            }
        }

        return output.toString();
    }

    private boolean isValidCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return false;
        }

        // Define strict patterns for each allowed command
        Map<String, String> allowedPatterns = Map.of(
            "nc -zv ", "^nc -zv [a-zA-Z0-9.-]+ [0-9]{1,5}$",
            "nslookup ", "^nslookup [a-zA-Z0-9.-]+$",
            "curl -v ", "^curl -v (https?://)?[a-zA-Z0-9.-]+(:[0-9]{1,5})?(/[a-zA-Z0-9.-]*)*$",
            "ping ", "^ping -c 4 [a-zA-Z0-9.-]+$",
            "telnet ", "^telnet [a-zA-Z0-9.-]+ [0-9]{1,5}$",
            "dig ", "^dig [a-zA-Z0-9.-]+ (A|AAAA|MX|NS|TXT)?$"
        );

        for (Map.Entry<String, String> entry : allowedPatterns.entrySet()) {
            if (command.startsWith(entry.getKey()) && 
                command.matches(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private String formatOutput(String output) {
        // 출력 길이를 500자로 제한
        if (output.length() > 1500) {
            output = output.substring(0, 1497) + "...";
        }
        
        // 모든 제어 문자와 특수 문자를 적절히 처리
        String sanitized = output
            .replace("\\", "\\\\")  // 백슬래시 먼저 이스케이프
            .replace("\"", "\\\"")  // 큰따옴표 이스케이프
            .replace("\n", "\\n")   // 줄바꿈 이스케이프
            .replace("\r", "\\r")   // 캐리지 리턴 이스케이프
            .replace("\t", "\\t")   // 탭 이스케이프
            .replace("\b", "\\b")   // 백스페이스 이스케이프
            .replace("\f", "\\f");  // 폼피드 이스케이프
        
        // 기타 제어 문자 제거 (ASCII 0-31 범위의 문자)
        sanitized = sanitized.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        
        return String.format("{\"output\": \"%s\"}", sanitized);
    }

    private ProcessBuilder createProcessBuilder(String command) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        
        // Set clean environment
        processBuilder.environment().clear();
        processBuilder.environment().put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin");
        processBuilder.environment().put("LANG", "en_US.UTF-8");
        
        // Set working directory
        processBuilder.directory(new File("/tmp"));
        
        // Merge error stream with input stream
        processBuilder.redirectErrorStream(true);
        
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            processBuilder.command("cmd.exe", "/c", command);
        } else {
            processBuilder.command("sh", "-c", command);
        }
        
        return processBuilder;
    }
} 