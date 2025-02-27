package k8sExample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            while ((line = errorReader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        return output.toString();
    }

    private boolean isValidCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return false;
        }

        // 허용되는 명령어 목록 확장
        String[] allowedCommands = {
            "nc -zv ",
            "nslookup ",
            "curl -v ",
            "ping ",
            "telnet ",
            "dig "
        };

        for (String allowedCommand : allowedCommands) {
            if (command.startsWith(allowedCommand)) {
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
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            processBuilder.command("cmd.exe", "/c", command);
        } else {
            processBuilder.command("sh", "-c", command);
        }
        return processBuilder;
    }
} 