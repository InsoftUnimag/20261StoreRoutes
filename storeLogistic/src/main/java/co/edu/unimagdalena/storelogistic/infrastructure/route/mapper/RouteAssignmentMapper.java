package co.edu.unimagdalena.storelogistic.infrastructure.route.mapper;

import co.edu.unimagdalena.storelogistic.domain.route.models.Order;
import co.edu.unimagdalena.storelogistic.domain.route.models.Route;
import co.edu.unimagdalena.storelogistic.domain.route.models.RouteVehicle;
import co.edu.unimagdalena.storelogistic.domain.route.models.Stop;
import co.edu.unimagdalena.storelogistic.domain.route.values.*;
import co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.jpa.*;
import co.edu.unimagdalena.storelogistic.infrastructure.route.web.dto.AssignOrderResponse;
import co.edu.unimagdalena.storelogistic.infrastructure.route.web.dto.StopResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class RouteAssignmentMapper {

    // ── JPA → Domain ──────────────────────────────────────────────────────────

    public Route toRoute(RouteJpaEntity e) {
        if (e == null) return null;
        List<Stop> stops = e.getStops() == null ? List.of() :
                e.getStops().stream().map(this::toStop).toList();
        return Route.reconstitute(
                e.getRouteId(),
                e.getVehicleId(),
                RouteCapacity.of(e.getTotalCapacityKg()),
                e.getAccumulatedWeightKg(),
                RouteStatus.valueOf(e.getStatus()),
                e.getDispatchDate(),
                stops
        );
    }

    public Stop toStop(StopJpaEntity e) {
        if (e == null) return null;
        return Stop.reconstitute(
                e.getStopId(),
                e.getRouteId(),
                e.getOrderId(),
                e.getSequence(),
                e.getDeliveryAddress(),
                StopStatus.valueOf(e.getStatus()),
                e.getDeliveryDate(),
                e.getCustomerContact()
        );
    }

    public Order toOrder(OrderJpaEntity e) {
        if (e == null) return null;
        return new Order(
                e.getOrderId(),
                LogisticWeight.of(e.getLogisticWeight()),
                e.getDeliveryAddress()
        );
    }

    public RouteVehicle toRouteVehicle(RouteVehicleJpaEntity e) {
        if (e == null) return null;
        VehicleType type = resolveVehicleType(e.getCategory().getTipo());
        return new RouteVehicle(
                e.getVehicleId(),
                type,
                RouteCapacity.of(e.getLoadCapacityKg())
        );
    }

    // ── Domain → JPA ──────────────────────────────────────────────────────────

    public RouteJpaEntity toRouteEntity(Route r) {
        if (r == null) return null;
        return RouteJpaEntity.builder()
                .routeId(r.routeId())
                .vehicleId(r.vehicleId())
                .totalCapacityKg(r.totalCapacity().valueKg())
                .accumulatedWeightKg(r.accumulatedWeightKg())
                .status(r.status().name())
                .dispatchDate(r.dispatchDate())
                .build();
    }

    public StopJpaEntity toStopEntity(Stop s) {
        if (s == null) return null;
        return StopJpaEntity.builder()
                .stopId(s.stopId())
                .routeId(s.routeId())
                .orderId(s.orderId())
                .sequence(s.sequence())
                .deliveryAddress(s.deliveryAddress())
                .status(s.status().name())
                .deliveryDate(s.deliveryDate())
                .customerContact(s.customerContact())
                .build();
    }

    public OrderJpaEntity toOrderEntity(Order o) {
        if (o == null) return null;
        return OrderJpaEntity.builder()
                .orderId(o.orderId())
                .logisticWeight(o.logisticWeight().valueKg())
                .deliveryAddress(o.deliveryAddress())
                .build();
    }

    // ── Domain → Web DTO ──────────────────────────────────────────────────────

    public AssignOrderResponse toAssignOrderResponse(Route route) {
        if (route == null) return null;
        Stop lastStop = route.stops().isEmpty() ? null : route.stops().get(route.stops().size() - 1);
        return AssignOrderResponse.builder()
                .routeId(route.routeId())
                .vehicleId(route.vehicleId())
                .accumulatedWeightKg(route.accumulatedWeightKg())
                .totalCapacityKg(route.totalCapacity().valueKg())
                .occupancyPercentage(route.occupancyPercentage())
                .routeStatus(route.status().name())
                .dispatchDate(route.dispatchDate())
                .stop(toStopResponse(lastStop))
                .build();
    }

    public StopResponse toStopResponse(Stop s) {
        if (s == null) return null;
        return StopResponse.builder()
                .stopId(s.stopId())
                .orderId(s.orderId())
                .sequence(s.sequence())
                .deliveryAddress(s.deliveryAddress())
                .stopStatus(s.status().name())
                .deliveryDate(s.deliveryDate())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VehicleType resolveVehicleType(String categoryName) {
        for (VehicleType t : VehicleType.values()) {
            if (t.categoryName().equals(categoryName)) return t;
        }
        return VehicleType.REGIONAL_SEMI;
    }
}
