package co.edu.unimagdalena.storelogistic.paymentmethod.unit.domain.models;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.models.OrderPaymentMethod;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.values.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class OrderPaymentMethodTest {

    @Test
    @DisplayName("of → creates CONTRA_ENTREGA with total correctly")
    void of_contraEntrega_createsCorrectly() {
        OrderPaymentMethod result = OrderPaymentMethod.of(1L, PaymentMethod.CONTRA_ENTREGA, new BigDecimal("150000.00"));
        assertThat(result.orderId()).isEqualTo(1L);
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CONTRA_ENTREGA);
        assertThat(result.totalPedido()).isEqualByComparingTo("150000.00");
    }

    @Test
    @DisplayName("of → creates CARTERA_COMERCIAL with null total correctly")
    void of_carteraComercial_createsCorrectly() {
        OrderPaymentMethod result = OrderPaymentMethod.of(2L, PaymentMethod.CARTERA_COMERCIAL, null);
        assertThat(result.orderId()).isEqualTo(2L);
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CARTERA_COMERCIAL);
        assertThat(result.totalPedido()).isNull();
    }

    @Test
    @DisplayName("of → null orderId throws IllegalArgumentException")
    void of_nullOrderId_throwsException() {
        assertThatThrownBy(() -> OrderPaymentMethod.of(null, PaymentMethod.CONTRA_ENTREGA, new BigDecimal("150000.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId must not be null");
    }

    @Test
    @DisplayName("of → null paymentMethod throws IllegalArgumentException")
    void of_nullPaymentMethod_throwsException() {
        assertThatThrownBy(() -> OrderPaymentMethod.of(1L, null, new BigDecimal("150000.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paymentMethod must not be null");
    }
}