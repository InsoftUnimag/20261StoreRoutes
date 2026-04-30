package co.edu.unimagdalena.storelogistic.domain.route.models;

import co.edu.unimagdalena.storelogistic.domain.route.exceptions.CapacityExceededException;
import co.edu.unimagdalena.storelogistic.domain.route.exceptions.InvalidStateTransitionException;
import co.edu.unimagdalena.storelogistic.domain.route.values.LogisticWeight;
import co.edu.unimagdalena.storelogistic.domain.route.values.RouteCapacity;
import co.edu.unimagdalena.storelogistic.domain.route.values.RouteStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Route {

    private Long routeId;
    private Long vehicleId;
    private RouteCapacity totalCapacity;
    private BigDecimal accumulatedWeightKg;
    private RouteStatus status;
    private LocalDate dispatchDate;
    private List<Stop> stops;

    private Route() {}

    public static Route createNew(Long vehicleId, RouteCapacity totalCapacity, LocalDate dispatchDate) {
        Route route = new Route();
        route.vehicleId          = vehicleId;
        route.totalCapacity      = totalCapacity;
        route.accumulatedWeightKg = BigDecimal.ZERO;
        route.status             = vehicleId != null ? RouteStatus.AVAILABLE : RouteStatus.PENDING_VEHICLE;
        route.dispatchDate       = dispatchDate;
        route.stops              = new ArrayList<>();
        return route;
    }

    public static Route reconstitute(Long routeId, Long vehicleId, RouteCapacity totalCapacity,
                                     BigDecimal accumulatedWeightKg, RouteStatus status,
                                     LocalDate dispatchDate, List<Stop> stops) {
        Route route = new Route();
        route.routeId            = routeId;
        route.vehicleId          = vehicleId;
        route.totalCapacity      = totalCapacity;
        route.accumulatedWeightKg = accumulatedWeightKg;
        route.status             = status;
        route.dispatchDate       = dispatchDate;
        route.stops              = new ArrayList<>(stops);
        return route;
    }

    public boolean canAcceptWeight(LogisticWeight weight) {
        BigDecimal remaining = totalCapacity.valueKg().subtract(accumulatedWeightKg);
        return weight.valueKg().compareTo(remaining) <= 0;
    }

    public BigDecimal occupancyPercentage() {
        if (totalCapacity.valueKg().compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return accumulatedWeightKg
                .multiply(BigDecimal.valueOf(100))
                .divide(totalCapacity.valueKg(), 2, RoundingMode.HALF_UP);
    }

    public boolean isFull() {
        return occupancyPercentage().compareTo(BigDecimal.valueOf(95)) >= 0;
    }

    public void close() {
        if (!status.isValidTransition(RouteStatus.CLOSED))
            throw new InvalidStateTransitionException(status.invalidTransitionMessage(RouteStatus.CLOSED));
        this.status = RouteStatus.CLOSED;
    }

    public void activate() {
        if (!status.isValidTransition(RouteStatus.AVAILABLE))
            throw new InvalidStateTransitionException(status.invalidTransitionMessage(RouteStatus.AVAILABLE));
        this.status = RouteStatus.AVAILABLE;
    }

    public Stop assignOrder(Order order) {
        if (!canAcceptWeight(order.logisticWeight())) {
            throw new CapacityExceededException(
                "Order weight " + order.logisticWeight().valueKg() + " kg exceeds remaining capacity of " +
                totalCapacity.valueKg().subtract(accumulatedWeightKg) + " kg"
            );
        }
        this.accumulatedWeightKg = this.accumulatedWeightKg.add(order.logisticWeight().valueKg());
        Stop stop = Stop.create(this.routeId, order.orderId(), stops.size() + 1, order.deliveryAddress());
        this.stops.add(stop);
        return stop;
    }

    public Long routeId()               { return routeId; }
    public Long vehicleId()             { return vehicleId; }
    public RouteCapacity totalCapacity(){ return totalCapacity; }
    public BigDecimal accumulatedWeightKg() { return accumulatedWeightKg; }
    public RouteStatus status()         { return status; }
    public LocalDate dispatchDate()     { return dispatchDate; }
    public List<Stop> stops()           { return Collections.unmodifiableList(stops); }

    public void setRouteId(Long routeId) { this.routeId = routeId; }
}