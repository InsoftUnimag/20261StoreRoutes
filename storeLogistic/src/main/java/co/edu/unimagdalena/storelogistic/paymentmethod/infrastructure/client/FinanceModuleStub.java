package co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.client;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.FinanceServiceUnavailableException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.LogisticsException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.OrderNotFoundException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.PaymentMethodNotRegisteredException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.models.OrderPaymentMethod;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.ports.out.FinanceGatewayPort;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.values.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Temporary mock — replace with FinanceModuleClient when the Finance Module is available.
 */
@Slf4j
@Component
public class FinanceModuleStub implements FinanceGatewayPort {

    private static final Map<Long, OrderPaymentMethod> HAPPY_PATHS = Map.of(
            1L, OrderPaymentMethod.of(1L, PaymentMethod.CONTRA_ENTREGA),
            2L, OrderPaymentMethod.of(2L, PaymentMethod.CARTERA_COMERCIAL)
    );

    private static final Map<Long, Supplier<LogisticsException>> ERROR_CASES = Map.of(
            404L, () -> new OrderNotFoundException("Pedido no encontrado"),
            422L, () -> new PaymentMethodNotRegisteredException("El cliente no tiene forma de pago registrada"),
            503L, () -> new FinanceServiceUnavailableException(
                    "El Módulo Financiero no está disponible. Intente nuevamente más tarde.")
    );

    @Override
    public OrderPaymentMethod findByOrderId(Long orderId) {
        log.info("[STUB] Resolving payment method for orderId={}", orderId);

        Optional.ofNullable(ERROR_CASES.get(orderId))
                .map(Supplier::get)
                .ifPresent(ex -> { throw ex; });

        return Optional.ofNullable(HAPPY_PATHS.get(orderId))
                .orElse(OrderPaymentMethod.of(orderId, PaymentMethod.CONTRA_ENTREGA));
    }
}
