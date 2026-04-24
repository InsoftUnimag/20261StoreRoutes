package co.edu.unimagdalena.storelogistic.consultar.unit.domain.models;

import co.edu.unimagdalena.storelogistic.consultar.domain.models.Carrier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CarrierTest {

    @Test
    @DisplayName("isActive → returns true when status is ACTIVE")
    void isActive_whenActive_returnsTrue() {
        Carrier carrier = new Carrier(1L, "Juan", "ACTIVE", "juan@test.com");
        assertThat(carrier.isActive()).isTrue();
    }

    @Test
    @DisplayName("isActive → returns false when status is INACTIVE")
    void isActive_whenInactive_returnsFalse() {
        Carrier carrier = new Carrier(2L, "Pedro", "INACTIVE", "pedro@test.com");
        assertThat(carrier.isActive()).isFalse();
    }

    @Test
    @DisplayName("isActive → is case-insensitive")
    void isActive_caseInsensitive() {
        Carrier carrier = new Carrier(3L, "Ana", "active", "ana@test.com");
        assertThat(carrier.isActive()).isTrue();
    }

    @Test
    @DisplayName("isActive → returns false when status is blank")
    void isActive_whenBlankStatus_returnsFalse() {
        Carrier carrier = new Carrier(4L, "Luis", "", "luis@test.com");
        assertThat(carrier.isActive()).isFalse();
    }
}
