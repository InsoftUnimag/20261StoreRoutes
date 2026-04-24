package co.edu.unimagdalena.storelogistic.paymentmethod.unit.domain.values;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.values.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PaymentMethodTest {

    @Test
    @DisplayName("fromString → CONTRA_ENTREGA parses correctly")
    void fromString_contraEntrega_returnsEnum() {
        assertThat(PaymentMethod.fromString("CONTRA_ENTREGA")).isEqualTo(PaymentMethod.CONTRA_ENTREGA);
    }

    @Test
    @DisplayName("fromString → CARTERA_COMERCIAL parses correctly")
    void fromString_carteraComercial_returnsEnum() {
        assertThat(PaymentMethod.fromString("CARTERA_COMERCIAL")).isEqualTo(PaymentMethod.CARTERA_COMERCIAL);
    }

    @Test
    @DisplayName("fromString → case insensitive")
    void fromString_lowercase_returnsEnum() {
        assertThat(PaymentMethod.fromString("contra_entrega")).isEqualTo(PaymentMethod.CONTRA_ENTREGA);
    }

    @Test
    @DisplayName("fromString → unknown value throws IllegalArgumentException")
    void fromString_unknownValue_throwsException() {
        assertThatThrownBy(() -> PaymentMethod.fromString("EFECTIVO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown payment method");
    }
}
