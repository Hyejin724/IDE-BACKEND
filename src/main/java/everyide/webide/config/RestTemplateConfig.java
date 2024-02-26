//package everyide.webide.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.client.SimpleClientHttpRequestFactory;
//import org.springframework.web.client.RestTemplate;
//
//import java.net.InetSocketAddress;
//import java.net.Proxy;
//
//@Configuration
//public class RestTemplateConfig {
//
//    @Bean(name = "proxyRestTemplate")
//    public RestTemplate restTemplate() {
//        // 프록시 설
//        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("krmp-proxy.9rum.cc", 3128));
//        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
//        requestFactory.setProxy(proxy);
//        // RestTemplate 인스턴스 생성 및 반환
//        RestTemplate restTemplate = new RestTemplate(requestFactory);
//        return restTemplate;
//
//    }
//}