package co.edu.unimagdalena.storelogistic.infrastructure.fleet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class TransporterRestClientConfig {

    @Bean("transporterRestClient")
    public RestClient transporterRestClient(
            @Value("${logistics.transporter.service.url}") String baseUrl,
            @Value("${logistics.transporter.service.timeout-ms:3000}") long timeoutMs) {
        var requestFactory = ClientHttpRequestFactoryBuilder.simple()
                .withConnectTimeout(Duration.ofMillis(timeoutMs))
                .withReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
