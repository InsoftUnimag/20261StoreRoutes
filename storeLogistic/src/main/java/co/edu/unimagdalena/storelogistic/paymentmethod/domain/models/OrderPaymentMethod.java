package co.edu.unimagdalena.storelogistic.paymentmethod.domain.models;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.values.PaymentMethod;

import java.util.Optional;

public record OrderPaymentMethod(Long orderId, PaymentMethod paymentMethod) {

    public static OrderPaymentMethod of(Long orderId, PaymentMethod paymentMethod) {
        return new OrderPaymentMethod(
                Optional.ofNullable(orderId)
                        .orElseThrow(() -> new IllegalArgumentException("orderId must not be null")),
                Optional.ofNullable(paymentMethod)
                        .orElseThrow(() -> new IllegalArgumentException("paymentMethod must not be null"))
        );
    }
}
