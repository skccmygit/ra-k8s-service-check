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
            "curl -v ", "^curl -v https?://[a-zA-Z0-9.-]+(/[a-zA-Z0-9.-]*)*$",
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
        return String.format("{\"output\": \"%s\"}", 
            output.replace("\"", "\\\"").replace("\n", "\\n"));
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