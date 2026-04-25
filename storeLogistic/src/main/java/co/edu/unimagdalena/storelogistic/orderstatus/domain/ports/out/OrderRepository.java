package co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Order;

import java.util.Optional;

public interface OrderRepository {

    Optional<Order> findById(Long orderId);

    Order save(Order order);
}
