package co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.out;

import co.edu.unimagdalena.storelogistic.domain.orderstatus.models.Order;

import java.util.Optional;

public interface OrderRepository {

    Optional<Order> findById(Long orderId);

    Order save(Order order);
}
