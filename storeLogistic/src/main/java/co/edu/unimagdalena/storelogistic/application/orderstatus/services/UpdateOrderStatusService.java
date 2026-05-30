package co.edu.unimagdalena.storelogistic.application.orderstatus.services;

import co.edu.unimagdalena.storelogistic.domain.fleet.ports.out.VehicleRepository;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.exceptions.OrderNotFoundException;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.models.Alert;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.models.Order;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.models.OrderStatusAudit;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.in.UpdateOrderStatusUseCase;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.out.AlertRepository;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.out.OrderRepository;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.out.OrderStatusAuditRepository;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.out.OrderStatusEventPublisher;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.values.FinalStatus;
import co.edu.unimagdalena.storelogistic.domain.route.ports.out.RouteRepository;
import co.edu.unimagdalena.storelogistic.domain.route.ports.out.StopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateOrderStatusService implements UpdateOrderStatusUseCase {

    private final OrderRepository orderRepository;
    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final AlertRepository alertRepository;
    private final OrderStatusAuditRepository auditRepository;
    private final OrderStatusEventPublisher eventPublisher;

    @Override
    @Transactional
    public Order update(Long orderId, FinalStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        Long carrierId = resolveCarrierId(orderId);
        log.info("Updating order status: orderId={}, carrierId={}, status={}", orderId, carrierId, status);

        FinalStatus previousStatus = order.finalStatus();
        order.updateStatus(status, carrierId);

        orderRepository.save(order);
        log.info("Persisted status: orderId={}, estadoFinal={}, tasaEfectividad={}",
                orderId, status.displayName(), status.effectivenessRate().value());

        if (previousStatus != null) {
            OrderStatusAudit audit = new OrderStatusAudit(
                    null, orderId, previousStatus, status, carrierId, LocalDateTime.now());
            auditRepository.save(audit);
            log.info("Audit record saved: orderId={}, {} → {}", orderId, previousStatus, status);
        }

        if (order.requiresAlert()) {
            Alert alert = Alert.createFor(order);
            alertRepository.save(alert);
            log.info("Alert generated: orderId={}, type={}", orderId, alert.type());
        }

        try {
            eventPublisher.publish(orderId, status, carrierId);
            log.info("Event published to finance module: orderId={}", orderId);
        } catch (Exception ex) {
            log.warn("Failed to publish event for orderId={}: {}", orderId, ex.getMessage(), ex);
        }

        return order;
    }

    private Long resolveCarrierId(Long orderId) {
        var stop = stopRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order " + orderId + " is not assigned to any route stop"));

        var route = routeRepository.findById(stop.routeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Route " + stop.routeId() + " not found for order " + orderId));

        var vehicle = vehicleRepository.findById(route.vehicleId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vehicle " + route.vehicleId() + " not found for route " + route.routeId()));

        log.info("Resolved carrierId={} for orderId={} (routeId={}, vehicleId={})",
                vehicle.getTransporterId(), orderId, route.routeId(), route.vehicleId());

        return vehicle.getTransporterId();
    }
}
