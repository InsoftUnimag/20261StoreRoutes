package co.edu.unimagdalena.storelogistic.route.application.services;

import co.edu.unimagdalena.storelogistic.route.domain.models.Order;
import co.edu.unimagdalena.storelogistic.route.domain.models.Route;
import co.edu.unimagdalena.storelogistic.route.domain.ports.in.AssignOrderUseCase;
import co.edu.unimagdalena.storelogistic.route.domain.ports.in.ProcessRouteRequestUseCase;
import co.edu.unimagdalena.storelogistic.route.domain.ports.out.OrderRepository;
import co.edu.unimagdalena.storelogistic.route.domain.values.LogisticWeight;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessRouteRequestService implements ProcessRouteRequestUseCase {

    private final OrderRepository orderRepository;
    private final AssignOrderUseCase assignOrderUseCase;

    @Override
    @Transactional
    public Route process(Long orderId, BigDecimal logisticWeightKg, String deliveryAddress) {
        log.info("Processing route request: orderId={}, weight={} kg", orderId, logisticWeightKg);

        Order order = orderRepository.findById(orderId).orElseGet(() -> {
            log.info("Order {} not found locally — persisting from event data", orderId);
            Order newOrder = new Order(orderId, LogisticWeight.of(logisticWeightKg), deliveryAddress);
            return orderRepository.save(newOrder);
        });

        AssignOrderUseCase.AssignResult result = assignOrderUseCase.assign(order.orderId());

        log.info("Route request processed: orderId={}, routeId={}, isNew={}",
                orderId, result.route().routeId(), result.isNewRoute());
        return result.route();
    }
}