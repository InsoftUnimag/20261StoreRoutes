package co.edu.unimagdalena.storelogistic.infrastructure.paymentmethod.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean("financeWebClient")
    public WebClient financeWebClient(
            @Value("${finance.module.base-url}") String baseUrl,
            @Value("${finance.module.timeout-ms:3000}") long timeoutMs) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeoutMs));
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
