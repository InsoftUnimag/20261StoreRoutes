package co.edu.unimagdalena.storelogistic.paymentmethod.unit.domain.models;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.models.OrderPaymentMethod;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.values.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class OrderPaymentMethodTest {

    @Test
    @DisplayName("of → creates CONTRA_ENTREGA correctly")
    void of_contraEntrega_createsCorrectly() {
        OrderPaymentMethod result = OrderPaymentMethod.of(1L, PaymentMethod.CONTRA_ENTREGA);
        assertThat(result.orderId()).isEqualTo(1L);
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CONTRA_ENTREGA);
    }

    @Test
    @DisplayName("of → creates CARTERA_COMERCIAL correctly")
    void of_carteraComercial_createsCorrectly() {
        OrderPaymentMethod result = OrderPaymentMethod.of(2L, PaymentMethod.CARTERA_COMERCIAL);
        assertThat(result.orderId()).isEqualTo(2L);
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CARTERA_COMERCIAL);
    }

    @Test
    @DisplayName("of → null orderId throws IllegalArgumentException")
    void of_nullOrderId_throwsException() {
        assertThatThrownBy(() -> OrderPaymentMethod.of(null, PaymentMethod.CONTRA_ENTREGA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId must not be null");
    }

    @Test
    @DisplayName("of → null paymentMethod throws IllegalArgumentException")
    void of_nullPaymentMethod_throwsException() {
        assertThatThrownBy(() -> OrderPaymentMethod.of(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paymentMethod must not be null");
    }
}
