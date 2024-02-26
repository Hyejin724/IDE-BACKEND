//package everyide.webide;
//
//import java.net.InetSocketAddress;
//import java.net.Proxy;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.http.client.SimpleClientHttpRequestFactory;
//import org.springframework.web.client.RestTemplate;
//
//public class ProxyExample {
//
//    public static void main(String[] args) {
//        // 프록시 서버 설정
//        String PROXY_SERVER_HOST = "kf88999ca9f71a.user-app.krampoline.com/";
//        int PROXY_SERVER_PORT = 8080; // 예: 프록시 서버의 포트 번호
//
//        // 프록시 객체 생성
//        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_SERVER_HOST, PROXY_SERVER_PORT));
//
//        // 요청 팩토리에 프록시 설정
//        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
//        requestFactory.setProxy(proxy);
//
//        // RestTemplate 객체 생성
//        RestTemplate restTemplate = new RestTemplate(requestFactory);
//
//        // 프록시를 통한 HTTP 요청 실행
//        ResponseEntity<String> responseEntity = restTemplate.getForEntity("http://httpbin.org/get", String.class);
//
//            }
//}
//
