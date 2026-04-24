package co.edu.unimagdalena.storelogistic.paymentmethod.application.services;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.models.OrderPaymentMethod;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.ports.in.ConsultPaymentMethodUseCase;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.ports.out.FinanceGatewayPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultPaymentMethodService implements ConsultPaymentMethodUseCase {

    private final FinanceGatewayPort financeGatewayPort;

    @Override
    public OrderPaymentMethod consult(Long orderId) {
        log.info("Consulting payment method for orderId={}", orderId);
        OrderPaymentMethod result = financeGatewayPort.findByOrderId(orderId);
        log.info("Payment method for orderId={} is {}", orderId, result.paymentMethod());
        return result;
    }
}
