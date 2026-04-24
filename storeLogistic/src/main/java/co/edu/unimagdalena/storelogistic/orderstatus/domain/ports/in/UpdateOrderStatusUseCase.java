package co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.in;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Order;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;

public interface UpdateOrderStatusUseCase {

    Order update(Long orderId, Long carrierId, FinalStatus status);
}
