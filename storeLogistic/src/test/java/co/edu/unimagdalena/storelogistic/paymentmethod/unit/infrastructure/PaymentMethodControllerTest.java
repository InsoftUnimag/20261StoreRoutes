package co.edu.unimagdalena.storelogistic.paymentmethod.unit.infrastructure;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.FinanceServiceUnavailableException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.OrderNotFoundException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.PaymentMethodNotRegisteredException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.ports.in.ConsultPaymentMethodUseCase;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.exception.PaymentMethodExceptionHandler;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.mapper.PaymentMethodMapperImpl;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.web.controller.PaymentMethodController;
import co.edu.unimagdalena.storelogistic.paymentmethod.testdata.OrderPaymentMethodFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentMethodController.class)
@Import({PaymentMethodMapperImpl.class, PaymentMethodExceptionHandler.class})
class PaymentMethodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsultPaymentMethodUseCase consultPaymentMethodUseCase;

    @Test
    @DisplayName("GET /{id}/forma-pago → 200 CONTRA_ENTREGA")
    void get_contraEntrega_returns200() throws Exception {
        when(consultPaymentMethodUseCase.consult(1L)).thenReturn(OrderPaymentMethodFixture.contraEntrega());

        mockMvc.perform(get("/pedidos/1/forma-pago"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.paymentMethod").value("CONTRA_ENTREGA"));
    }

    @Test
    @DisplayName("GET /{id}/forma-pago → 200 CARTERA_COMERCIAL")
    void get_carteraComercial_returns200() throws Exception {
        when(consultPaymentMethodUseCase.consult(2L)).thenReturn(OrderPaymentMethodFixture.carteraComercial());

        mockMvc.perform(get("/pedidos/2/forma-pago"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod").value("CARTERA_COMERCIAL"));
    }

    @Test
    @DisplayName("GET /{id}/forma-pago → 404 when order not found")
    void get_orderNotFound_returns404() throws Exception {
        when(consultPaymentMethodUseCase.consult(999L)).thenThrow(new OrderNotFoundException("Pedido no encontrado"));

        mockMvc.perform(get("/pedidos/999/forma-pago"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PEDIDO_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("GET /{id}/forma-pago → 422 when payment not registered")
    void get_paymentNotRegistered_returns422() throws Exception {
        when(consultPaymentMethodUseCase.consult(5L))
                .thenThrow(new PaymentMethodNotRegisteredException("El cliente no tiene forma de pago registrada"));

        mockMvc.perform(get("/pedidos/5/forma-pago"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("FORMA_PAGO_NO_REGISTRADA"));
    }

    @Test
    @DisplayName("GET /{id}/forma-pago → 503 when service unavailable")
    void get_serviceUnavailable_returns503() throws Exception {
        when(consultPaymentMethodUseCase.consult(1L))
                .thenThrow(new FinanceServiceUnavailableException("El Módulo Financiero no está disponible."));

        mockMvc.perform(get("/pedidos/1/forma-pago"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("SERVICIO_FINANCIERO_NO_DISPONIBLE"));
    }

    @Test
    @DisplayName("GET /abc/forma-pago → 400 when id is non-numeric")
    void get_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/pedidos/abc/forma-pago"))
                .andExpect(status().isBadRequest());
    }
}
