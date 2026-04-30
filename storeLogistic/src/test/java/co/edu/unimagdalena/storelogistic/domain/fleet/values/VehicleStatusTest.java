package co.edu.unimagdalena.storelogistic.domain.fleet.values;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleStatusTest {

    @Test
    void isValidTransition_fromMaintenanceToAvailable_returnsTrue() {
        assertTrue(VehicleStatus.EN_MANTENIMIENTO.isValidTransition(VehicleStatus.DISPONIBLE));
    }

    @Test
    void isValidTransition_fromAvailableToOnRoute_returnsTrue() {
        assertTrue(VehicleStatus.DISPONIBLE.isValidTransition(VehicleStatus.EN_RUTA));
    }

    @Test
    void isValidTransition_fromOnRouteToAvailable_returnsTrue() {
        assertTrue(VehicleStatus.EN_RUTA.isValidTransition(VehicleStatus.DISPONIBLE));
    }

    @Test
    void isValidTransition_toOutOfService_fromAny_returnsTrue() {
        assertTrue(VehicleStatus.EN_MANTENIMIENTO.isValidTransition(VehicleStatus.FUERA_DE_SERVICIO));
        assertTrue(VehicleStatus.DISPONIBLE.isValidTransition(VehicleStatus.FUERA_DE_SERVICIO));
        assertTrue(VehicleStatus.EN_RUTA.isValidTransition(VehicleStatus.FUERA_DE_SERVICIO));
    }

    @Test
    void isValidTransition_fromOutOfServiceToMaintenance_returnsTrue() {
        assertTrue(VehicleStatus.FUERA_DE_SERVICIO.isValidTransition(VehicleStatus.EN_MANTENIMIENTO));
    }

    @Test
    void isValidTransition_sameState_returnsFalse() {
        assertFalse(VehicleStatus.EN_MANTENIMIENTO.isValidTransition(VehicleStatus.EN_MANTENIMIENTO));
        assertFalse(VehicleStatus.DISPONIBLE.isValidTransition(VehicleStatus.DISPONIBLE));
    }

    @Test
    void isValidTransition_fromMaintenanceToOnRoute_returnsFalse() {
        assertFalse(VehicleStatus.EN_MANTENIMIENTO.isValidTransition(VehicleStatus.EN_RUTA));
    }

    @Test
    void isValidTransition_fromOutOfServiceToAvailable_returnsFalse() {
        assertFalse(VehicleStatus.FUERA_DE_SERVICIO.isValidTransition(VehicleStatus.DISPONIBLE));
    }

    @Test
    void invalidTransitionMessage_containsBothStates() {
        String message = VehicleStatus.EN_MANTENIMIENTO.invalidTransitionMessage(VehicleStatus.EN_RUTA);
        assertNotNull(message);
        assertTrue(message.contains("EN_MANTENIMIENTO"));
        assertTrue(message.contains("EN_RUTA"));
    }
}