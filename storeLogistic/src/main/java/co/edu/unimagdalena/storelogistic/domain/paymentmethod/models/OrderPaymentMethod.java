package co.edu.unimagdalena.storelogistic.domain.paymentmethod.models;

import co.edu.unimagdalena.storelogistic.domain.paymentmethod.values.PaymentMethod;

import java.math.BigDecimal;
import java.util.Optional;

public record OrderPaymentMethod(Long orderId, PaymentMethod paymentMethod, BigDecimal totalPedido) {

    public static OrderPaymentMethod of(Long orderId, PaymentMethod paymentMethod, BigDecimal totalPedido) {
        return new OrderPaymentMethod(
                Optional.ofNullable(orderId)
                        .orElseThrow(() -> new IllegalArgumentException("orderId must not be null")),
                Optional.ofNullable(paymentMethod)
                        .orElseThrow(() -> new IllegalArgumentException("paymentMethod must not be null")),
                totalPedido
        );
    }
}
