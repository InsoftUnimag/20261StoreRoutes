package co.edu.unimagdalena.storelogistic.paymentmethod.domain.ports.out;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.models.OrderPaymentMethod;

public interface FinanceGatewayPort {
    OrderPaymentMethod findByOrderId(Long orderId);
}
