package co.edu.unimagdalena.storelogistic.route.domain.ports.out;

import co.edu.unimagdalena.storelogistic.route.domain.models.Order;

import java.util.Optional;

public interface OrderRepository {

    Optional<Order> findById(Long orderId);

    Order save(Order order);
}