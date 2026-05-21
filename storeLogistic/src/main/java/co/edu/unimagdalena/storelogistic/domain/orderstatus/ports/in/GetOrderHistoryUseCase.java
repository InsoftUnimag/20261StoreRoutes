package co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.in;

import co.edu.unimagdalena.storelogistic.domain.orderstatus.models.Order;

import java.util.List;

public interface GetOrderHistoryUseCase {

    List<Order> getByCarrier(Long carrierId);
}
