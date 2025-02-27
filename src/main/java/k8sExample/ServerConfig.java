package k8sExample;

import java.time.LocalDateTime;

public class ServerConfig {
    private static final String VERSION = "1.2";
    private final int port;
    private final String clusterEnv;
    private final String clusterName;
    private final LocalDateTime startTime;

    public ServerConfig() {
        this.port = Integer.parseInt(System.getenv().getOrDefault("PORT", "4567"));
        this.clusterEnv = System.getenv().getOrDefault("CLUSTER_ENV", "unknown");
        this.clusterName = System.getenv().getOrDefault("CLUSTER_NAME", "unknown");
        this.startTime = LocalDateTime.now();
    }

    public int getPort() { return port; }
    public String getVersion() { return VERSION; }
    public String getClusterEnv() { return clusterEnv; }
    public String getClusterName() { return clusterName; }
    public LocalDateTime getStartTime() { return startTime; }
} 