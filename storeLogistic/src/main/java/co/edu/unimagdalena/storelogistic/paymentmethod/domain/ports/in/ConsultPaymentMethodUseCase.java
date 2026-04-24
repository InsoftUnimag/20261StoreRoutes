package co.edu.unimagdalena.storelogistic.paymentmethod.domain.ports.in;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.models.OrderPaymentMethod;

public interface ConsultPaymentMethodUseCase {
    OrderPaymentMethod consult(Long orderId);
}
