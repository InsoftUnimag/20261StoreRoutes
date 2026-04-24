package co.edu.unimagdalena.storelogistic.paymentmethod.unit.infrastructure;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.models.OrderPaymentMethod;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.values.PaymentMethod;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.mapper.PaymentMethodMapper;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.mapper.PaymentMethodMapperImpl;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.web.dto.FinancePaymentMethodResponse;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.web.dto.PaymentMethodResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PaymentMethodMapperTest {

    private final PaymentMethodMapper mapper = new PaymentMethodMapperImpl();

    @Test
    @DisplayName("toDomain → maps CONTRA_ENTREGA DTO to domain")
    void toDomain_contraEntrega_mapsCorrectly() {
        FinancePaymentMethodResponse dto = new FinancePaymentMethodResponse(1L, "CONTRA_ENTREGA");
        OrderPaymentMethod result = mapper.toDomain(dto);
        assertThat(result.orderId()).isEqualTo(1L);
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CONTRA_ENTREGA);
    }

    @Test
    @DisplayName("toDomain → maps CARTERA_COMERCIAL DTO to domain")
    void toDomain_carteraComercial_mapsCorrectly() {
        FinancePaymentMethodResponse dto = new FinancePaymentMethodResponse(2L, "CARTERA_COMERCIAL");
        OrderPaymentMethod result = mapper.toDomain(dto);
        assertThat(result.orderId()).isEqualTo(2L);
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CARTERA_COMERCIAL);
    }

    @Test
    @DisplayName("toDomain → unknown forma_pago throws IllegalArgumentException")
    void toDomain_unknownFormaPago_throwsException() {
        FinancePaymentMethodResponse dto = new FinancePaymentMethodResponse(1L, "EFECTIVO");
        assertThatThrownBy(() -> mapper.toDomain(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toResponse → maps domain to PaymentMethodResponse")
    void toResponse_mapsCorrectly() {
        OrderPaymentMethod domain = OrderPaymentMethod.of(2L, PaymentMethod.CARTERA_COMERCIAL);
        PaymentMethodResponse response = mapper.toResponse(domain);
        assertThat(response.getOrderId()).isEqualTo(2L);
        assertThat(response.getPaymentMethod()).isEqualTo("CARTERA_COMERCIAL");
    }
}
