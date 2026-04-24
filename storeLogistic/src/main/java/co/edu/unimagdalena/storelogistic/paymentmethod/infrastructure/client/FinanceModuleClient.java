package co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.client;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.FinanceServiceUnavailableException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.OrderNotFoundException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.PaymentMethodNotRegisteredException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.models.OrderPaymentMethod;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.ports.out.FinanceGatewayPort;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.mapper.PaymentMethodMapper;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.web.dto.FinancePaymentMethodResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Real HTTP adapter — wire this as @Component and remove MockFinanceModuleClient when the Finance Module is available.
 */
@Slf4j
@RequiredArgsConstructor
public class FinanceModuleClient implements FinanceGatewayPort {

    private final @Qualifier("financeWebClient") WebClient webClient;
    private final @Qualifier("financeRetryTemplate") RetryTemplate retryTemplate;
    private final PaymentMethodMapper mapper;

    private static final Map<HttpStatus, Supplier<RuntimeException>> CLIENT_ERROR_MAP = Map.of(
            HttpStatus.NOT_FOUND,
            () -> new OrderNotFoundException("Pedido no encontrado"),
            HttpStatus.UNPROCESSABLE_ENTITY,
            () -> new PaymentMethodNotRegisteredException("El cliente no tiene forma de pago registrada")
    );

    private static final Set<Class<? extends Throwable>> NON_RETRYABLE_EXCEPTIONS = Set.of(
            OrderNotFoundException.class,
            PaymentMethodNotRegisteredException.class
    );

    @Override
    public OrderPaymentMethod findByOrderId(Long orderId) {
        log.info("Calling Finance Module for orderId={}", orderId);
        return retryTemplate.execute(
                context -> {
                    Optional.of(context.getRetryCount())
                            .filter(count -> count > 0)
                            .ifPresent(count -> log.warn("Retry attempt {}/3 for orderId={}, cause: {}",
                                    count, orderId, Optional.ofNullable(context.getLastThrowable())
                                            .map(Throwable::getMessage).orElse("unknown")));
                    return callFinanceModule(orderId);
                },
                context -> {
                    Optional.ofNullable(context.getLastThrowable())
                            .filter(ex -> NON_RETRYABLE_EXCEPTIONS.contains(ex.getClass()))
                            .map(RuntimeException.class::cast)
                            .ifPresent(ex -> { throw ex; });
                    log.error("Finance service unavailable after {} attempts for orderId={}", context.getRetryCount(), orderId);
                    throw new FinanceServiceUnavailableException(
                            "El Módulo Financiero no está disponible. Intente nuevamente más tarde.");
                }
        );
    }

    private OrderPaymentMethod callFinanceModule(Long orderId) {
        try {
            FinancePaymentMethodResponse dto = webClient.get()
                    .uri("/pedidos/{id}/forma-pago", orderId)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError(),
                            clientResponse -> reactor.core.publisher.Mono.error(
                                    Optional.ofNullable(CLIENT_ERROR_MAP.get(HttpStatus.valueOf(clientResponse.statusCode().value())))
                                            .map(Supplier::get)
                                            .<RuntimeException>orElse(new IllegalArgumentException("Unexpected client error: " + clientResponse.statusCode()))))
                    .onStatus(
                            status -> status.is5xxServerError(),
                            clientResponse -> reactor.core.publisher.Mono.error(
                                    new RuntimeException("Finance service returned 5xx: " + clientResponse.statusCode())))
                    .bodyToMono(FinancePaymentMethodResponse.class)
                    .block();

            return Optional.ofNullable(dto)
                    .map(mapper::toDomain)
                    .orElseThrow(() -> new FinanceServiceUnavailableException("Empty response from Finance Module"));
        } catch (WebClientRequestException ex) {
            log.warn("Network error calling Finance Module for orderId={}: {}", orderId, ex.getMessage());
            throw new RuntimeException("Network error: " + ex.getMessage(), ex);
        }
    }
}
