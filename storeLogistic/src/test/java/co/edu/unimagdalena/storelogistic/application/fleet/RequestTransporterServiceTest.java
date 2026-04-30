package co.edu.unimagdalena.storelogistic.application.fleet;

import co.edu.unimagdalena.storelogistic.application.fleet.services.RequestTransporterService;
import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.InvalidTransporterException;
import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.TransporterNotAvailableException;
import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.VehicleNotFoundException;
import co.edu.unimagdalena.storelogistic.domain.fleet.models.Vehicle;
import co.edu.unimagdalena.storelogistic.domain.fleet.ports.out.TransporterServicePort;
import co.edu.unimagdalena.storelogistic.domain.fleet.ports.out.VehicleRepository;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.LoadCapacity;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.VehicleStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestTransporterServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private TransporterServicePort transporterServicePort;

    @InjectMocks
    private RequestTransporterService service;

    private Vehicle buildVehicle(Long id) {
        return Vehicle.builder()
                .vehicleId(id)
                .loadCapacity(new LoadCapacity(BigDecimal.valueOf(1500)))
                .status(VehicleStatus.DISPONIBLE)
                .transporterId(0L)
                .currentWeight(BigDecimal.ZERO)
                .build();
    }

    @Test
    void request_happyPath_assignsTransporterAndSaves() {
        var vehicle = buildVehicle(1L);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(transporterServicePort.getAvailable()).thenReturn(1L);
        when(vehicleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.request(1L);

        assertThat(result.getTransporterId()).isEqualTo(1L);
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void request_callsInOrder_getAvailable_validateExistence_save() {
        var vehicle = buildVehicle(1L);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(transporterServicePort.getAvailable()).thenReturn(1L);
        when(vehicleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.request(1L);

        InOrder inOrder = inOrder(transporterServicePort, vehicleRepository);
        inOrder.verify(transporterServicePort).getAvailable();
        inOrder.verify(transporterServicePort).validateExistence(1L);
        inOrder.verify(vehicleRepository).save(any());
    }

    @Test
    void request_vehicleNotFound_throwsVehicleNotFoundException() {
        when(vehicleRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.request(99L))
                .isInstanceOf(VehicleNotFoundException.class);

        verify(vehicleRepository, never()).save(any());
        verifyNoInteractions(transporterServicePort);
    }

    @Test
    void request_noTransporterAvailable_propagatesException_andDoesNotSave() {
        var vehicle = buildVehicle(1L);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(transporterServicePort.getAvailable()).thenThrow(new TransporterNotAvailableException());

        assertThatThrownBy(() -> service.request(1L))
                .isInstanceOf(TransporterNotAvailableException.class);

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void request_invalidTransporter_propagatesException_andDoesNotSave() {
        var vehicle = buildVehicle(1L);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(transporterServicePort.getAvailable()).thenReturn(999L);
        doThrow(new InvalidTransporterException(999L))
                .when(transporterServicePort).validateExistence(999L);

        assertThatThrownBy(() -> service.request(1L))
                .isInstanceOf(InvalidTransporterException.class);

        verify(vehicleRepository, never()).save(any());
    }
}