package co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.config;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.OrderNotFoundException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.PaymentMethodNotRegisteredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@EnableRetry
@Configuration
public class RetryConfig {

    @Bean("financeRetryTemplate")
    public RetryTemplate financeRetryTemplate(
            @Value("${finance.module.retry.max-attempts:3}") int maxAttempts,
            @Value("${finance.module.retry.initial-interval-ms:500}") long initialIntervalMs,
            @Value("${finance.module.retry.multiplier:2.0}") double multiplier) {

        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(OrderNotFoundException.class, false);
        retryableExceptions.put(PaymentMethodNotRegisteredException.class, false);

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
                log.warn("Finance module call failed (attempt {}/{}): {}", context.getRetryCount(), maxAttempts, throwable.getMessage());
            }
        });
        return template;
    }
}
