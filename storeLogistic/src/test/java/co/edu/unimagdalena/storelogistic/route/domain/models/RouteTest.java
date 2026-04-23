package co.edu.unimagdalena.storelogistic.route.domain.models;

import co.edu.unimagdalena.storelogistic.route.domain.exceptions.CapacityExceededException;
import co.edu.unimagdalena.storelogistic.route.domain.values.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class RouteTest {

    private static final RouteCapacity CAPACITY_1500 = RouteCapacity.of(1_500.0);
    private static final LocalDate TODAY = LocalDate.now();

    private Route emptyRoute;

    @BeforeEach
    void setUp() {
        emptyRoute = Route.reconstitute(1L, 10L, CAPACITY_1500,
                BigDecimal.ZERO, RouteStatus.AVAILABLE, TODAY, List.of());
    }

    // ── canAcceptWeight ────────────────────────────────────────────────────────

    @Test
    @DisplayName("canAcceptWeight → true when weight fits exactly")
    void canAcceptWeight_exactFit_returnsTrue() {
        assertThat(emptyRoute.canAcceptWeight(LogisticWeight.of(1_500.0))).isTrue();
    }

    @Test
    @DisplayName("canAcceptWeight → true when weight is under capacity")
    void canAcceptWeight_underCapacity_returnsTrue() {
        assertThat(emptyRoute.canAcceptWeight(LogisticWeight.of(1_000.0))).isTrue();
    }

    @Test
    @DisplayName("canAcceptWeight → false when weight exceeds remaining")
    void canAcceptWeight_exceedsRemaining_returnsFalse() {
        assertThat(emptyRoute.canAcceptWeight(LogisticWeight.of(1_501.0))).isFalse();
    }

    // ── occupancyPercentage ────────────────────────────────────────────────────

    @Test
    @DisplayName("occupancyPercentage → 0% when empty")
    void occupancyPercentage_emptyRoute_isZero() {
        assertThat(emptyRoute.occupancyPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("occupancyPercentage → 50% when half full")
    void occupancyPercentage_halfFull_is50() {
        Route route = Route.reconstitute(1L, 10L, CAPACITY_1500,
                BigDecimal.valueOf(750), RouteStatus.AVAILABLE, TODAY, List.of());
        assertThat(route.occupancyPercentage()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    @DisplayName("occupancyPercentage → 95% at threshold")
    void occupancyPercentage_atThreshold_is95() {
        Route route = Route.reconstitute(1L, 10L, CAPACITY_1500,
                BigDecimal.valueOf(1_425), RouteStatus.AVAILABLE, TODAY, List.of());
        assertThat(route.occupancyPercentage()).isEqualByComparingTo(BigDecimal.valueOf(95));
    }

    @Test
    @DisplayName("occupancyPercentage → 100% when full")
    void occupancyPercentage_full_is100() {
        Route route = Route.reconstitute(1L, 10L, CAPACITY_1500,
                BigDecimal.valueOf(1_500), RouteStatus.AVAILABLE, TODAY, List.of());
        assertThat(route.occupancyPercentage()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    // ── isFull ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isFull → false at 94.9%")
    void isFull_below95_returnsFalse() {
        Route route = Route.reconstitute(1L, 10L, CAPACITY_1500,
                BigDecimal.valueOf(1_423), RouteStatus.AVAILABLE, TODAY, List.of());
        assertThat(route.isFull()).isFalse();
    }

    @Test
    @DisplayName("isFull → true at exactly 95%")
    void isFull_exactlyAt95_returnsTrue() {
        Route route = Route.reconstitute(1L, 10L, CAPACITY_1500,
                BigDecimal.valueOf(1_425), RouteStatus.AVAILABLE, TODAY, List.of());
        assertThat(route.isFull()).isTrue();
    }

    @Test
    @DisplayName("isFull → true at 100%")
    void isFull_at100_returnsTrue() {
        Route route = Route.reconstitute(1L, 10L, CAPACITY_1500,
                BigDecimal.valueOf(1_500), RouteStatus.AVAILABLE, TODAY, List.of());
        assertThat(route.isFull()).isTrue();
    }

    // ── assignOrder ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("assignOrder → creates Stop and updates accumulated weight")
    void assignOrder_happyPath_createsStopAndUpdatesWeight() {
        Order order = new Order(42L, LogisticWeight.of(500.0), "Calle 1 #2-3");

        Stop stop = emptyRoute.assignOrder(order);

        assertThat(stop).isNotNull();
        assertThat(stop.orderId()).isEqualTo(42L);
        assertThat(stop.sequence()).isEqualTo(1);
        assertThat(stop.deliveryAddress()).isEqualTo("Calle 1 #2-3");
        assertThat(emptyRoute.accumulatedWeightKg()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(emptyRoute.stops()).hasSize(1);
    }

    @Test
    @DisplayName("assignOrder → second stop gets sequence 2")
    void assignOrder_secondStop_getsSequence2() {
        Order first  = new Order(1L, LogisticWeight.of(300.0), "Addr 1");
        Order second = new Order(2L, LogisticWeight.of(200.0), "Addr 2");

        emptyRoute.assignOrder(first);
        Stop stop2 = emptyRoute.assignOrder(second);

        assertThat(stop2.sequence()).isEqualTo(2);
    }

    @Test
    @DisplayName("assignOrder → throws CapacityExceededException when no space")
    void assignOrder_noCapacity_throwsException() {
        Order order = new Order(1L, LogisticWeight.of(2_000.0), "Addr");
        assertThatThrownBy(() -> emptyRoute.assignOrder(order))
                .isInstanceOf(CapacityExceededException.class);
    }

    // ── close ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("close → sets status to CLOSED")
    void close_changesStatusToClosed() {
        emptyRoute.close();
        assertThat(emptyRoute.status()).isEqualTo(RouteStatus.CLOSED);
    }

    // ── createNew ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createNew with vehicle → status AVAILABLE")
    void createNew_withVehicle_isAvailable() {
        Route route = Route.createNew(1L, CAPACITY_1500, TODAY);
        assertThat(route.status()).isEqualTo(RouteStatus.AVAILABLE);
        assertThat(route.accumulatedWeightKg()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("createNew without vehicle → status PENDING_VEHICLE")
    void createNew_withoutVehicle_isPendingVehicle() {
        Route route = Route.createNew(null, CAPACITY_1500, TODAY);
        assertThat(route.status()).isEqualTo(RouteStatus.PENDING_VEHICLE);
    }
}
