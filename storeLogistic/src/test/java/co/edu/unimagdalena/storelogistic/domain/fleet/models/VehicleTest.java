package co.edu.unimagdalena.storelogistic.domain.fleet.models;

import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.InvalidStateTransitionException;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.CategoryType;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.LoadCapacity;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.VehicleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class VehicleTest {

    private Category category;
    private LoadCapacity loadCapacity;
    private Long transporterId;

    @BeforeEach
    void setUp() {
        category = new Category(1L, CategoryType.CAMIONETA_URBANA,
                new LoadCapacity(BigDecimal.valueOf(1500)));
        loadCapacity = new LoadCapacity(BigDecimal.valueOf(1500));
        transporterId = 1L;
    }

    @Test
    void createNew_setsInitialState() {
        var vehicle = Vehicle.createNew(category, loadCapacity, transporterId);

        assertNotNull(vehicle);
        assertEquals(VehicleStatus.EN_MANTENIMIENTO, vehicle.getStatus());
        assertNotNull(vehicle.getCreatedAt());
        assertEquals(transporterId, vehicle.getTransporterId());
    }

    @Test
    void changeStatus_validTransition_updatesStatus() {
        var vehicle = Vehicle.createNew(category, loadCapacity, transporterId);

        vehicle.changeStatus(VehicleStatus.DISPONIBLE);

        assertEquals(VehicleStatus.DISPONIBLE, vehicle.getStatus());
        assertNotNull(vehicle.getUpdatedAt());
    }

    @Test
    void changeStatus_invalidTransition_throwsException() {
        var vehicle = Vehicle.createNew(category, loadCapacity, transporterId);

        assertThrows(InvalidStateTransitionException.class,
                () -> vehicle.changeStatus(VehicleStatus.EN_RUTA));
    }

    @Test
    void assignTransporter_updatesTransporterId() {
        var vehicle = Vehicle.createNew(category, loadCapacity, 1L);

        vehicle.assignTransporter(2L);

        assertEquals(2L, vehicle.getTransporterId());
        assertNotNull(vehicle.getUpdatedAt());
    }

    @Test
    void assignTransporter_withNoExistingTransporter_setsId() {
        var vehicle = Vehicle.builder()
                .loadCapacity(loadCapacity)
                .status(VehicleStatus.EN_MANTENIMIENTO)
                .build();

        vehicle.assignTransporter(1L);

        assertEquals(1L, vehicle.getTransporterId());
    }

    @Test
    void assignTransporter_reassignment_overwritesPreviousId() {
        var vehicle = Vehicle.createNew(category, loadCapacity, 1L);
        vehicle.assignTransporter(2L);

        vehicle.assignTransporter(3L);

        assertEquals(3L, vehicle.getTransporterId());
    }
}