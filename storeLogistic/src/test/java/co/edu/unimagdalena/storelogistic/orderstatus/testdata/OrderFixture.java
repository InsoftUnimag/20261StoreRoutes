package co.edu.unimagdalena.storelogistic.orderstatus.testdata;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Order;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;

import java.time.LocalDateTime;

public final class OrderFixture {

    private OrderFixture() {}

    public static Order withoutStatus() {
        return Order.create(1L, 10L, null);
    }

    public static Order withStatus(FinalStatus status, Long carrierId) {
        Order order = Order.create(1L, 10L, null);
        order.updateStatus(status, carrierId);
        return order;
    }

    public static Order restored(Long orderId, FinalStatus status, Long carrierId) {
        return Order.restore(
                orderId, 10L, carrierId,
                status,
                status != null ? status.effectivenessRate() : null,
                LocalDateTime.now().minusDays(1),
                status != null ? LocalDateTime.now() : null
        );
    }
}
