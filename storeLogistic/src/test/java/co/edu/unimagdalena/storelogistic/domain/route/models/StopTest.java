package co.edu.unimagdalena.storelogistic.domain.route.models;

import co.edu.unimagdalena.storelogistic.domain.route.exceptions.InvalidStateTransitionException;
import co.edu.unimagdalena.storelogistic.domain.route.values.StopStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class StopTest {

    private Stop pendingStop;

    @BeforeEach
    void setUp() {
        pendingStop = Stop.create(1L, 10L, 1, "Calle Test");
    }

    // ── markDelivered ────────────────────────────────────────────────────────

    @Test
    @DisplayName("markDelivered → PENDING to DELIVERED is valid")
    void markDelivered_fromPending_succeeds() {
        pendingStop.markDelivered(LocalDate.now());
        assertThat(pendingStop.status()).isEqualTo(StopStatus.DELIVERED);
        assertThat(pendingStop.deliveryDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("markDelivered → throws when already DELIVERED")
    void markDelivered_alreadyDelivered_throwsInvalidTransition() {
        pendingStop.markDelivered(LocalDate.now());
        assertThatThrownBy(() -> pendingStop.markDelivered(LocalDate.now()))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("DELIVERED");
    }

    @Test
    @DisplayName("markDelivered → throws when REJECTED")
    void markDelivered_fromRejected_throwsInvalidTransition() {
        pendingStop.markRejected();
        assertThatThrownBy(() -> pendingStop.markDelivered(LocalDate.now()))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("REJECTED");
    }

    // ── markRejected ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("markRejected → PENDING to REJECTED is valid")
    void markRejected_fromPending_succeeds() {
        pendingStop.markRejected();
        assertThat(pendingStop.status()).isEqualTo(StopStatus.REJECTED);
    }

    @Test
    @DisplayName("markRejected → throws when already REJECTED")
    void markRejected_alreadyRejected_throwsInvalidTransition() {
        pendingStop.markRejected();
        assertThatThrownBy(pendingStop::markRejected)
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("REJECTED");
    }

    @Test
    @DisplayName("markRejected → throws when DELIVERED")
    void markRejected_fromDelivered_throwsInvalidTransition() {
        pendingStop.markDelivered(LocalDate.now());
        assertThatThrownBy(pendingStop::markRejected)
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("DELIVERED");
    }
}