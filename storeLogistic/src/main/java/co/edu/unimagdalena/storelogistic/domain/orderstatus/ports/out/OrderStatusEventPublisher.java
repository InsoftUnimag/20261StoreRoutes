package co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.out;

import co.edu.unimagdalena.storelogistic.domain.orderstatus.values.FinalStatus;

public interface OrderStatusEventPublisher {

    void publish(Long orderId, FinalStatus status, Long carrierId);
}
