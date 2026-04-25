package co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;

public interface OrderStatusEventPublisher {

    void publish(Long orderId, FinalStatus status, Long carrierId);
}
