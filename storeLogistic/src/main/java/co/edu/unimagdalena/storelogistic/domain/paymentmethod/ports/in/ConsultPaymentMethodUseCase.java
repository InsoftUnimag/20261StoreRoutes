package co.edu.unimagdalena.storelogistic.domain.paymentmethod.ports.in;

import co.edu.unimagdalena.storelogistic.domain.paymentmethod.models.OrderPaymentMethod;

public interface ConsultPaymentMethodUseCase {
    OrderPaymentMethod consult(Long orderId);
}
