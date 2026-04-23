package co.edu.unimagdalena.storelogistic.fleet.domain.values;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransporterStatusTest {

    @Test
    void availableValue_exists() {
        assertNotNull(TransporterStatus.AVAILABLE);
    }

    @Test
    void notAvailableValue_exists() {
        assertNotNull(TransporterStatus.NOT_AVAILABLE);
    }

    @Test
    void values_containsBothStatuses() {
        assertEquals(2, TransporterStatus.values().length);
    }

    @Test
    void valueOf_available_returnsCorrectEnum() {
        assertEquals(TransporterStatus.AVAILABLE, TransporterStatus.valueOf("AVAILABLE"));
    }

    @Test
    void valueOf_notAvailable_returnsCorrectEnum() {
        assertEquals(TransporterStatus.NOT_AVAILABLE, TransporterStatus.valueOf("NOT_AVAILABLE"));
    }
}