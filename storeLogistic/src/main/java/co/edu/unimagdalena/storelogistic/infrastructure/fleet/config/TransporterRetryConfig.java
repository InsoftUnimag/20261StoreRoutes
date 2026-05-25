package co.edu.unimagdalena.storelogistic.infrastructure.fleet.config;

import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.InvalidTransporterException;
import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.TransporterNotAvailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class TransporterRetryConfig {

    @Bean("transporterRetryTemplate")
    public RetryTemplate transporterRetryTemplate(
            @Value("${logistics.transporter.service.retry.max-attempts:3}") int maxAttempts,
            @Value("${logistics.transporter.service.retry.initial-interval-ms:500}") long initialIntervalMs,
            @Value("${logistics.transporter.service.retry.multiplier:2.0}") double multiplier) {

        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(TransporterNotAvailableException.class, false);
        retryableExceptions.put(InvalidTransporterException.class, false);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(maxAttempts, retryableExceptions, true, true);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialIntervalMs);
        backOffPolicy.setMultiplier(multiplier);

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOffPolicy);
        template.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                log.warn("Transporter module call failed (attempt {}/{}): {}", context.getRetryCount(), maxAttempts, throwable.getMessage());
            }
        });
        return template;
    }
}
