package co.edu.unimagdalena.storelogistic.paymentmethod.unit.application;

import co.edu.unimagdalena.storelogistic.paymentmethod.application.services.ConsultPaymentMethodService;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.FinanceServiceUnavailableException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.OrderNotFoundException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.models.OrderPaymentMethod;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.ports.out.FinanceGatewayPort;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.values.PaymentMethod;
import co.edu.unimagdalena.storelogistic.paymentmethod.testdata.OrderPaymentMethodFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultPaymentMethodServiceTest {

    @Mock
    private FinanceGatewayPort financeGatewayPort;

    @InjectMocks
    private ConsultPaymentMethodService service;

    @Test
    @DisplayName("consult → returns CONTRA_ENTREGA from gateway")
    void consult_contraEntrega_returnsResult() {
        when(financeGatewayPort.findByOrderId(1L)).thenReturn(OrderPaymentMethodFixture.contraEntrega());
        OrderPaymentMethod result = service.consult(1L);
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CONTRA_ENTREGA);
        assertThat(result.orderId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("consult → returns CARTERA_COMERCIAL from gateway")
    void consult_carteraComercial_returnsResult() {
        when(financeGatewayPort.findByOrderId(2L)).thenReturn(OrderPaymentMethodFixture.carteraComercial());
        OrderPaymentMethod result = service.consult(2L);
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CARTERA_COMERCIAL);
    }

    @Test
    @DisplayName("consult → propagates OrderNotFoundException without wrapping")
    void consult_orderNotFound_propagatesException() {
        when(financeGatewayPort.findByOrderId(999L)).thenThrow(new OrderNotFoundException("Pedido no encontrado"));
        assertThatThrownBy(() -> service.consult(999L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Pedido no encontrado");
    }

    @Test
    @DisplayName("consult → propagates FinanceServiceUnavailableException without wrapping")
    void consult_serviceUnavailable_propagatesException() {
        when(financeGatewayPort.findByOrderId(1L)).thenThrow(new FinanceServiceUnavailableException("unavailable"));
        assertThatThrownBy(() -> service.consult(1L))
                .isInstanceOf(FinanceServiceUnavailableException.class);
    }
}
