package co.edu.unimagdalena.storelogistic.domain.paymentmethod.ports.out;

import co.edu.unimagdalena.storelogistic.domain.paymentmethod.models.OrderPaymentMethod;

public interface FinanceGatewayPort {
    OrderPaymentMethod findByOrderId(Long orderId);
}
