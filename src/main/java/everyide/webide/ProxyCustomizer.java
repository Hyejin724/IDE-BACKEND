package everyide.webide;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

class ProxyCustomizer implements RestTemplateCustomizer {

    @Override
    public void customize(RestTemplate restTemplate) {
        HttpHost proxy = new HttpHost(PROXY_SERVER_HOST, 8000);
        HttpClient httpClient = HttpClientBuilder.create()
                .setRoutePlanner(new DefaultProxyRoutePlanner(proxy) {
                    @Override
                    public HttpHost determineProxy(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
                        return super.determineProxy(target, request, context);
                    }
                })
                .build();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
    }
}
