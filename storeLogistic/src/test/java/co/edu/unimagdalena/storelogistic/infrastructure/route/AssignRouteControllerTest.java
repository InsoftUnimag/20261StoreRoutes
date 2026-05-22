package co.edu.unimagdalena.storelogistic.infrastructure.route;

import co.edu.unimagdalena.storelogistic.domain.route.exceptions.OrderNotFoundException;
import co.edu.unimagdalena.storelogistic.domain.route.models.*;
import co.edu.unimagdalena.storelogistic.domain.route.ports.in.AssignOrderUseCase;
import co.edu.unimagdalena.storelogistic.domain.route.ports.in.AssignOrderUseCase.AssignResult;
import co.edu.unimagdalena.storelogistic.domain.route.ports.in.GetRoutesUseCase;
import co.edu.unimagdalena.storelogistic.domain.route.values.*;
import co.edu.unimagdalena.storelogistic.infrastructure.route.mapper.RouteAssignmentMapper;
import co.edu.unimagdalena.storelogistic.infrastructure.route.web.controller.AssignRouteController;
import co.edu.unimagdalena.storelogistic.infrastructure.route.web.dto.AssignOrderResponse;
import co.edu.unimagdalena.storelogistic.infrastructure.route.web.dto.StopResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssignRouteController.class)
@Import(co.edu.unimagdalena.storelogistic.infrastructure.route.exception.RouteExceptionHandler.class)
class AssignRouteControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean AssignOrderUseCase assignOrderUseCase;
    @MockBean GetRoutesUseCase getRoutesUseCase;
    @MockBean RouteAssignmentMapper mapper;

    private Route sampleRoute() {
        return Route.reconstitute(10L, 5L, RouteCapacity.of(1_500.0),
                BigDecimal.valueOf(500), RouteStatus.AVAILABLE, LocalDate.now(), List.of());
    }

    private AssignOrderResponse sampleResponse() {
        return AssignOrderResponse.builder()
                .routeId(10L).vehicleId(5L)
                .accumulatedWeightKg(BigDecimal.valueOf(500))
                .totalCapacityKg(BigDecimal.valueOf(1_500))
                .occupancyPercentage(BigDecimal.valueOf(33.33))
                .routeStatus("AVAILABLE")
                .dispatchDate(LocalDate.now())
                .stop(StopResponse.builder().stopId(1L).orderId(1L).sequence(1).build())
                .build();
    }

    @Test
    @DisplayName("POST /assignments with existing route → HTTP 200")
    void assignOrder_existingRoute_returns200() throws Exception {
        Route route = sampleRoute();
        when(assignOrderUseCase.assign(1L)).thenReturn(new AssignResult(route, false));
        when(mapper.toAssignOrderResponse(route)).thenReturn(sampleResponse());

        mockMvc.perform(post("/logistics/routes/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(10))
                .andExpect(jsonPath("$.routeStatus").value("AVAILABLE"));
    }

    @Test
    @DisplayName("POST /assignments with new route → HTTP 201")
    void assignOrder_newRoute_returns201() throws Exception {
        Route route = sampleRoute();
        when(assignOrderUseCase.assign(1L)).thenReturn(new AssignResult(route, true));
        when(mapper.toAssignOrderResponse(route)).thenReturn(sampleResponse());

        mockMvc.perform(post("/logistics/routes/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":1}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /assignments with null orderId → HTTP 400")
    void assignOrder_nullOrderId_returns400() throws Exception {
        mockMvc.perform(post("/logistics/routes/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /assignments with non-existent order → HTTP 404")
    void assignOrder_orderNotFound_returns404() throws Exception {
        when(assignOrderUseCase.assign(99L)).thenThrow(new OrderNotFoundException(99L));

        mockMvc.perform(post("/logistics/routes/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":99}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ORDER_NOT_FOUND"));
    }
}
