package co.edu.unimagdalena.storelogistic.paymentmethod.testdata;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.models.OrderPaymentMethod;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.values.PaymentMethod;

import java.math.BigDecimal;

public final class OrderPaymentMethodFixture {
    private OrderPaymentMethodFixture() {}

    public static OrderPaymentMethod contraEntrega() {
        return OrderPaymentMethod.of(1L, PaymentMethod.CONTRA_ENTREGA, new BigDecimal("150000.00"));
    }

    public static OrderPaymentMethod carteraComercial() {
        return OrderPaymentMethod.of(2L, PaymentMethod.CARTERA_COMERCIAL, null);
    }

    public static OrderPaymentMethod withId(Long orderId) {
        return OrderPaymentMethod.of(orderId, PaymentMethod.CONTRA_ENTREGA, new BigDecimal("150000.00"));
    }
}
