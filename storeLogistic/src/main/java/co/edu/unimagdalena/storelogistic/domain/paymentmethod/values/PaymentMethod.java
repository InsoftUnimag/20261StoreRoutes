package co.edu.unimagdalena.storelogistic.domain.paymentmethod.values;

import java.util.Arrays;

public enum PaymentMethod {
    CONTRA_ENTREGA,
    CARTERA_COMERCIAL;

    public static PaymentMethod fromString(String value) {
        return Arrays.stream(values())
                .filter(pm -> pm.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment method: " + value));
    }
}
