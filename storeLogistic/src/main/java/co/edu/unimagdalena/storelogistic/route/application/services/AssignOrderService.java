package co.edu.unimagdalena.storelogistic.route.application.services;

import co.edu.unimagdalena.storelogistic.route.domain.exceptions.OrderNotFoundException;
import co.edu.unimagdalena.storelogistic.route.domain.models.Order;
import co.edu.unimagdalena.storelogistic.route.domain.models.Route;
import co.edu.unimagdalena.storelogistic.route.domain.models.RouteVehicle;
import co.edu.unimagdalena.storelogistic.route.domain.models.Stop;
import co.edu.unimagdalena.storelogistic.route.domain.ports.in.AssignOrderUseCase;
import co.edu.unimagdalena.storelogistic.route.domain.ports.out.OrderRepository;
import co.edu.unimagdalena.storelogistic.route.domain.ports.out.RouteRepository;
import co.edu.unimagdalena.storelogistic.route.domain.ports.out.StopRepository;
import co.edu.unimagdalena.storelogistic.route.domain.values.RouteCapacity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignOrderService implements AssignOrderUseCase {

    private final OrderRepository orderRepository;
    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final SelectVehicleService selectVehicleService;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AssignResult assign(Long orderId) {
        log.info("Starting route assignment for orderId={}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        Optional<Route> existingRoute = routeRepository.findAvailableWithCapacity(order.logisticWeight());

        if (existingRoute.isPresent()) {
            Route route = existingRoute.get();
            log.info("Found existing route id={}, current occupancy={}%",
                    route.routeId(), route.occupancyPercentage());

            Stop stop = route.assignOrder(order);
            routeRepository.save(route);
            Stop saved = stopRepository.save(stop);
            stop.setStopId(saved.stopId());

            if (route.isFull()) {
                log.info("Route id={} reached {}% occupancy — closing", route.routeId(), route.occupancyPercentage());
                route.close();
                routeRepository.save(route);
            } else if (route.occupancyPercentage().doubleValue() >= 90) {
                log.warn("Route id={} at {}% occupancy — approaching closure threshold",
                        route.routeId(), route.occupancyPercentage());
            }

            log.info("Assigned orderId={} to existing routeId={}, accumulatedWeight={} kg, occupancy={}%",
                    orderId, route.routeId(), route.accumulatedWeightKg(), route.occupancyPercentage());
            return new AssignResult(route, false);
        }

        return createNewRoute(order);
    }

    private AssignResult createNewRoute(Order order) {
        Optional<RouteVehicle> vehicle = selectVehicleService.select(order.logisticWeight());

        if (vehicle.isPresent()) {
            RouteVehicle v = vehicle.get();
            Route newRoute = Route.createNew(v.vehicleId(), v.capacity(), LocalDate.now());
            Route saved = routeRepository.save(newRoute);
            newRoute.setRouteId(saved.routeId());

            log.info("Created new route id={} with vehicle id={} type={} capacity={} kg",
                    newRoute.routeId(), v.vehicleId(), v.type(), v.capacity().valueKg());

            Stop stop = newRoute.assignOrder(order);
            routeRepository.save(newRoute);
            Stop savedStop = stopRepository.save(stop);
            stop.setStopId(savedStop.stopId());

            if (newRoute.isFull()) {
                log.info("New route id={} immediately reached {}% — closing", newRoute.routeId(), newRoute.occupancyPercentage());
                newRoute.close();
                routeRepository.save(newRoute);
            }

            log.info("Assigned orderId={} to new routeId={}", order.orderId(), newRoute.routeId());
            return new AssignResult(newRoute, true);
        }

        log.warn("No vehicles available — creating PENDING_VEHICLE route for orderId={}", order.orderId());
        RouteCapacity defaultCapacity = RouteCapacity.of(order.logisticWeight().valueKg().multiply(java.math.BigDecimal.TWO));
        Route pendingRoute = Route.createNew(null, defaultCapacity, LocalDate.now());
        Route saved = routeRepository.save(pendingRoute);
        pendingRoute.setRouteId(saved.routeId());
        return new AssignResult(pendingRoute, true);
    }
}