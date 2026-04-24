package co.edu.unimagdalena.storelogistic.consultar.unit.infrastructure;

import co.edu.unimagdalena.storelogistic.consultar.domain.exceptions.AccessDeniedException;
import co.edu.unimagdalena.storelogistic.consultar.domain.ports.in.QueryStopsUseCase;
import co.edu.unimagdalena.storelogistic.consultar.infrastructure.exception.ConsultarExceptionHandler;
import co.edu.unimagdalena.storelogistic.consultar.infrastructure.mapper.QueryStopsMapper;
import co.edu.unimagdalena.storelogistic.consultar.infrastructure.web.controller.QueryStopsController;
import co.edu.unimagdalena.storelogistic.consultar.infrastructure.web.dto.QueryStopsResponse;
import co.edu.unimagdalena.storelogistic.consultar.testdata.StopFixture;
import co.edu.unimagdalena.storelogistic.route.domain.models.Stop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QueryStopsController.class)
@Import(ConsultarExceptionHandler.class)
class QueryStopsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    QueryStopsUseCase queryStopsUseCase;

    @MockitoBean
    QueryStopsMapper queryStopsMapper;

    @Test
    @DisplayName("GET /logistics/routes/{id}/stops → 200 with valid params")
    void getStops_validParams_returns200() throws Exception {
        List<Stop> stops = List.of(StopFixture.withSequence(1));
        QueryStopsResponse response = new QueryStopsResponse(1L, 1L, 1, List.of());
        when(queryStopsUseCase.query(1L, 1L)).thenReturn(stops);
        when(queryStopsMapper.toResponse(1L, 1L, stops)).thenReturn(response);

        mockMvc.perform(get("/logistics/routes/1/stops").param("carrierId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /logistics/routes/{id}/stops → 403 when access denied")
    void getStops_accessDenied_returns403() throws Exception {
        when(queryStopsUseCase.query(1L, 99L)).thenThrow(new AccessDeniedException());

        mockMvc.perform(get("/logistics/routes/1/stops").param("carrierId", "99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"))
                .andExpect(jsonPath("$.mensaje").value("Acceso denegado"));
    }

    @Test
    @DisplayName("GET /logistics/routes/{id}/stops → 400 when carrierId is missing")
    void getStops_missingCarrierId_returns400() throws Exception {
        mockMvc.perform(get("/logistics/routes/1/stops"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /logistics/routes/{id}/stops → 400 when routeId is non-numeric")
    void getStops_nonNumericRouteId_returns400() throws Exception {
        mockMvc.perform(get("/logistics/routes/abc/stops").param("carrierId", "1"))
                .andExpect(status().isBadRequest());
    }
}
