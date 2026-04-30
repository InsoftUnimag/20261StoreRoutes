package co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.in;

import co.edu.unimagdalena.storelogistic.domain.orderstatus.models.Order;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.values.FinalStatus;

public interface UpdateOrderStatusUseCase {

    Order update(Long orderId, Long carrierId, FinalStatus status);
}
