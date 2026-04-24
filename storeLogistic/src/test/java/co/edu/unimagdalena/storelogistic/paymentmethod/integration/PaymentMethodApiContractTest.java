package co.edu.unimagdalena.storelogistic.paymentmethod.integration;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.ports.out.FinanceGatewayPort;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.FinanceServiceUnavailableException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.OrderNotFoundException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.PaymentMethodNotRegisteredException;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentMethodController.class)
@Import({PaymentMethodMapperImpl.class, PaymentMethodExceptionHandler.class})
class PaymentMethodApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private co.edu.unimagdalena.storelogistic.paymentmethod.domain.ports.in.ConsultPaymentMethodUseCase consultPaymentMethodUseCase;

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /pedidos/1/forma-pago → 200 with orderId and paymentMethod fields")
    void contract_contraEntrega_200WithCorrectShape() throws Exception {
        when(consultPaymentMethodUseCase.consult(1L)).thenReturn(OrderPaymentMethodFixture.contraEntrega());

        mockMvc.perform(get("/pedidos/1/forma-pago").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.paymentMethod").value("CONTRA_ENTREGA"));
    }

    @Test
    @DisplayName("GET /pedidos/2/forma-pago → 200 CARTERA_COMERCIAL")
    void contract_carteraComercial_200() throws Exception {
        when(consultPaymentMethodUseCase.consult(2L)).thenReturn(OrderPaymentMethodFixture.carteraComercial());

        mockMvc.perform(get("/pedidos/2/forma-pago").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(2))
                .andExpect(jsonPath("$.paymentMethod").value("CARTERA_COMERCIAL"));
    }

    // ── Error contracts ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /pedidos/999/forma-pago → 404 with PEDIDO_NO_ENCONTRADO error body")
    void contract_orderNotFound_404ErrorShape() throws Exception {
        when(consultPaymentMethodUseCase.consult(999L))
                .thenThrow(new OrderNotFoundException("Pedido no encontrado"));

        mockMvc.perform(get("/pedidos/999/forma-pago").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo").value("PEDIDO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /pedidos/5/forma-pago → 422 with FORMA_PAGO_NO_REGISTRADA error body")
    void contract_paymentNotRegistered_422ErrorShape() throws Exception {
        when(consultPaymentMethodUseCase.consult(5L))
                .thenThrow(new PaymentMethodNotRegisteredException("El cliente no tiene forma de pago registrada"));

        mockMvc.perform(get("/pedidos/5/forma-pago").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo").value("FORMA_PAGO_NO_REGISTRADA"))
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /pedidos/10/forma-pago → 503 with SERVICIO_FINANCIERO_NO_DISPONIBLE error body")
    void contract_serviceUnavailable_503ErrorShape() throws Exception {
        when(consultPaymentMethodUseCase.consult(10L))
                .thenThrow(new FinanceServiceUnavailableException("El Módulo Financiero no está disponible."));

        mockMvc.perform(get("/pedidos/10/forma-pago").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo").value("SERVICIO_FINANCIERO_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /pedidos/abc/forma-pago → 400 for non-numeric id")
    void contract_nonNumericId_400() throws Exception {
        mockMvc.perform(get("/pedidos/abc/forma-pago").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ── Response shape enforcement ────────────────────────────────────────────

    @Test
    @DisplayName("200 response must NOT expose idPedido or forma_pago (internal field names)")
    void contract_successResponse_doesNotExposeInternalFieldNames() throws Exception {
        when(consultPaymentMethodUseCase.consult(1L)).thenReturn(OrderPaymentMethodFixture.contraEntrega());

        mockMvc.perform(get("/pedidos/1/forma-pago").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPedido").doesNotExist())
                .andExpect(jsonPath("$.forma_pago").doesNotExist())
                .andExpect(jsonPath("$.id_pedido").doesNotExist());
    }
}
