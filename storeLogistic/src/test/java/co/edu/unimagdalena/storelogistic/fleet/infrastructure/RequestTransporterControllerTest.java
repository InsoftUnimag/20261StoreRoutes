package co.edu.unimagdalena.storelogistic.fleet.infrastructure;

import co.edu.unimagdalena.storelogistic.fleet.domain.exceptions.InvalidTransporterException;
import co.edu.unimagdalena.storelogistic.fleet.domain.exceptions.TransporterNotAvailableException;
import co.edu.unimagdalena.storelogistic.fleet.domain.exceptions.VehicleNotFoundException;
import co.edu.unimagdalena.storelogistic.fleet.domain.models.Vehicle;
import co.edu.unimagdalena.storelogistic.fleet.domain.ports.in.RequestTransporterUseCase;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.LoadCapacity;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.VehicleStatus;
import co.edu.unimagdalena.storelogistic.fleet.infrastructure.exception.GlobalExceptionHandler;
import co.edu.unimagdalena.storelogistic.fleet.infrastructure.mapper.VehicleTransporterMapperImpl;
import co.edu.unimagdalena.storelogistic.fleet.infrastructure.web.controller.RequestTransporterController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RequestTransporterController.class)
@Import({VehicleTransporterMapperImpl.class, GlobalExceptionHandler.class})
class RequestTransporterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestTransporterUseCase requestTransporterUseCase;

    private Vehicle buildVehicle(Long id, Long transporterId) {
        return Vehicle.builder()
                .vehicleId(id)
                .transporterId(transporterId)
                .loadCapacity(new LoadCapacity(BigDecimal.valueOf(1500)))
                .status(VehicleStatus.DISPONIBLE)
                .currentWeight(BigDecimal.ZERO)
                .build();
    }

    @Test
    void post_vehicleExistsAndTransporterAvailable_returns200() throws Exception {
        when(requestTransporterUseCase.request(1L)).thenReturn(buildVehicle(1L, 1L));

        mockMvc.perform(post("/logistics/vehicles/1/transporter")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idVehiculo").value(1))
                .andExpect(jsonPath("$.idTransportista").value(1));
    }

    @Test
    void post_vehicleNotFound_returns404() throws Exception {
        when(requestTransporterUseCase.request(99L)).thenThrow(new VehicleNotFoundException(99L));

        mockMvc.perform(post("/logistics/vehicles/99/transporter")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("VEHICULO_NOT_FOUND"));
    }

    @Test
    void post_noTransporterAvailable_returns409() throws Exception {
        when(requestTransporterUseCase.request(1L)).thenThrow(new TransporterNotAvailableException());

        mockMvc.perform(post("/logistics/vehicles/1/transporter")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSPORTISTA_NO_DISPONIBLE"));
    }

    @Test
    void post_invalidTransporter_returns400() throws Exception {
        when(requestTransporterUseCase.request(1L)).thenThrow(new InvalidTransporterException(0L));

        mockMvc.perform(post("/logistics/vehicles/1/transporter")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TRANSPORTISTA_INVALIDO"));
    }
}