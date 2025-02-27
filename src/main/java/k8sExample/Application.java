package k8sExample;

public class Application {
    public static void main(String[] args) {
        NetworkDiagnosticServer server = new NetworkDiagnosticServer();
        server.start();
    }
} 