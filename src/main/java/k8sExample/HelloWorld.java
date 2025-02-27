package k8sExample;
import static spark.Spark.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HelloWorld {
    //App 시작시간
    private static final LocalDateTime startTime = LocalDateTime.now();

    public static void main(String[] args) {
        //시작 시간 출력
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println("App start time: " + startTime.format(formatter));


        // 요청 로깅 추가 - 여기에 추가 
        before((req, res) -> { 
            System.out.println("Received request: " + req.requestMethod() + " " + req.pathInfo()); 
        }); 
        // CORS 설정 
        before((req, res) -> { 
            res.header("Access-Control-Allow-Origin", "*"); 
            res.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS"); 
            res.header("Access-Control-Allow-Headers", "*"); 
        });



        get("/", (req, res) -> {
            InetAddress ip = null;
            try {
                ip = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            return "Hello World! <br> Server IP Address:" + ip.getHostAddress() + "<br>" + "Version 1.2 <br>" + "App Start Time: " + startTime.format(formatter); 
        });

        // post test endpoint
        post("/message", (req, res)  -> {
            System.out.println("message PathInfo:" + req.pathInfo());
            String body = req.body(); // 요청 본문 가져오기
            res.type("application/json; charset=UTF-8");
            return "{\"message\":\"받은데이터: " + body + "\"}";
        });
    }

}
