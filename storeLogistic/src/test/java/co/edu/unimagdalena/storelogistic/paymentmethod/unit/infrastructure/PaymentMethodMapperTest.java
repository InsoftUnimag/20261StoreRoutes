package co.edu.unimagdalena.storelogistic.paymentmethod.unit.infrastructure;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.models.OrderPaymentMethod;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.values.PaymentMethod;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.mapper.PaymentMethodMapper;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.mapper.PaymentMethodMapperImpl;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.web.dto.FinancePaymentMethodResponse;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.web.dto.PaymentMethodResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class PaymentMethodMapperTest {

    private final PaymentMethodMapper mapper = new PaymentMethodMapperImpl();

    @Test
    @DisplayName("toDomain → maps CONTRA_ENTREGA DTO with total to domain")
    void toDomain_contraEntrega_mapsCorrectly() {
        FinancePaymentMethodResponse dto = new FinancePaymentMethodResponse(1L, "CONTRA_ENTREGA", new BigDecimal("150000.00"));
        OrderPaymentMethod result = mapper.toDomain(dto);
        assertThat(result.orderId()).isEqualTo(1L);
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CONTRA_ENTREGA);
        assertThat(result.totalPedido()).isEqualByComparingTo("150000.00");
    }

    @Test
    @DisplayName("toDomain → maps CARTERA_COMERCIAL DTO with null total to domain")
    void toDomain_carteraComercial_mapsCorrectly() {
        FinancePaymentMethodResponse dto = new FinancePaymentMethodResponse(2L, "CARTERA_COMERCIAL", null);
        OrderPaymentMethod result = mapper.toDomain(dto);
        assertThat(result.orderId()).isEqualTo(2L);
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CARTERA_COMERCIAL);
        assertThat(result.totalPedido()).isNull();
    }

    @Test
    @DisplayName("toDomain → unknown forma_pago throws IllegalArgumentException")
    void toDomain_unknownFormaPago_throwsException() {
        FinancePaymentMethodResponse dto = new FinancePaymentMethodResponse(1L, "EFECTIVO", null);
        assertThatThrownBy(() -> mapper.toDomain(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toResponse → maps CARTERA_COMERCIAL domain with null total to response")
    void toResponse_carteraComercial_mapsCorrectly() {
        OrderPaymentMethod domain = OrderPaymentMethod.of(2L, PaymentMethod.CARTERA_COMERCIAL, null);
        PaymentMethodResponse response = mapper.toResponse(domain);
        assertThat(response.getOrderId()).isEqualTo(2L);
        assertThat(response.getPaymentMethod()).isEqualTo("CARTERA_COMERCIAL");
        assertThat(response.getTotalPedido()).isNull();
    }

    @Test
    @DisplayName("toResponse → maps CONTRA_ENTREGA domain with total to response")
    void toResponse_contraEntrega_mapsCorrectly() {
        OrderPaymentMethod domain = OrderPaymentMethod.of(1L, PaymentMethod.CONTRA_ENTREGA, new BigDecimal("150000.00"));
        PaymentMethodResponse response = mapper.toResponse(domain);
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getPaymentMethod()).isEqualTo("CONTRA_ENTREGA");
        assertThat(response.getTotalPedido()).isEqualByComparingTo("150000.00");
    }
}