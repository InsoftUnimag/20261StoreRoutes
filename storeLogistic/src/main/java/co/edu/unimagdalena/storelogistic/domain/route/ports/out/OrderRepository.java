package co.edu.unimagdalena.storelogistic.domain.route.ports.out;

import co.edu.unimagdalena.storelogistic.domain.route.models.Order;

import java.util.Optional;

public interface OrderRepository {

    Optional<Order> findById(Long orderId);

    Order save(Order order);
}