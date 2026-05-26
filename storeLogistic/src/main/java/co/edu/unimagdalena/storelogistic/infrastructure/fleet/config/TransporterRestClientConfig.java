package co.edu.unimagdalena.storelogistic.infrastructure.fleet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class TransporterRestClientConfig {

    @Bean("transporterRestClient")
    public RestClient transporterRestClient(
            @Value("${logistics.transporter.service.url}") String baseUrl,
            @Value("${logistics.transporter.service.timeout-ms:3000}") long timeoutMs) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeoutMs);
        requestFactory.setReadTimeout((int) timeoutMs);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
