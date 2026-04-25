package co.edu.unimagdalena.storelogistic.orderstatus.unit.infrastructure;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.exceptions.CarrierNotFoundException;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.exceptions.OrderNotFoundException;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.in.UpdateOrderStatusUseCase;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.exception.OrderStatusExceptionHandler;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.mapper.OrderStatusMapperImpl;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.web.controller.UpdateOrderStatusController;
import co.edu.unimagdalena.storelogistic.orderstatus.testdata.OrderFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UpdateOrderStatusController.class)
@Import({OrderStatusMapperImpl.class, OrderStatusExceptionHandler.class})
class UpdateOrderStatusControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @Test
    @DisplayName("PUT /logistics/orders/{id}/status → 200 with correct tasaEfectividad")
    void put_validRequest_returns200() throws Exception {
        var order = OrderFixture.withStatus(FinalStatus.ENTREGADO_COMPLETO, 50L);
        when(updateOrderStatusUseCase.update(eq(1L), eq(50L), eq(FinalStatus.ENTREGADO_COMPLETO)))
                .thenReturn(order);

        mockMvc.perform(put("/logistics/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idTransportista":50,"estadoFinal":"Entregado Completo"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPedido").value(1))
                .andExpect(jsonPath("$.estadoFinal").value("Entregado Completo"))
                .andExpect(jsonPath("$.tasaEfectividad").value(100))
                .andExpect(jsonPath("$.idTransportista").value(50));
    }

    @Test
    @DisplayName("PUT with invalid estadoFinal → 422 ESTADO_FINAL_INVALIDO")
    void put_invalidEstadoFinal_returns422() throws Exception {
        mockMvc.perform(put("/logistics/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idTransportista":50,"estadoFinal":"EstadoInventado"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("ESTADO_FINAL_INVALIDO"));
    }

    @Test
    @DisplayName("PUT with null idTransportista → 400 VALIDACION_FALLIDA")
    void put_nullIdTransportista_returns400() throws Exception {
        mockMvc.perform(put("/logistics/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estadoFinal":"Entregado Completo"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));
    }

    @Test
    @DisplayName("PUT with order not found → 404 PEDIDO_NO_ENCONTRADO")
    void put_orderNotFound_returns404() throws Exception {
        when(updateOrderStatusUseCase.update(any(), any(), any()))
                .thenThrow(new OrderNotFoundException(99L));

        mockMvc.perform(put("/logistics/orders/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idTransportista":50,"estadoFinal":"Entregado Completo"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PEDIDO_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("PUT with carrier not found → 404 TRANSPORTISTA_NO_ENCONTRADO")
    void put_carrierNotFound_returns404() throws Exception {
        when(updateOrderStatusUseCase.update(any(), any(), any()))
                .thenThrow(new CarrierNotFoundException(99L));

        mockMvc.perform(put("/logistics/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idTransportista":99,"estadoFinal":"Entregado Completo"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("TRANSPORTISTA_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("PUT RECHAZO_PARCIAL → tasaEfectividad=80")
    void put_rechazoParcial_rate80() throws Exception {
        var order = OrderFixture.withStatus(FinalStatus.RECHAZO_PARCIAL, 50L);
        when(updateOrderStatusUseCase.update(eq(2L), eq(50L), eq(FinalStatus.RECHAZO_PARCIAL)))
                .thenReturn(order);

        mockMvc.perform(put("/logistics/orders/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idTransportista":50,"estadoFinal":"Rechazo Parcial"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasaEfectividad").value(80));
    }
}
