package co.edu.unimagdalena.storelogistic.orderstatus.application.services;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.exceptions.CarrierNotFoundException;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.exceptions.OrderNotFoundException;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Alert;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Order;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.OrderStatusAudit;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.in.UpdateOrderStatusUseCase;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out.AlertRepository;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out.CarrierRepository;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out.OrderRepository;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out.OrderStatusAuditRepository;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out.OrderStatusEventPublisher;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;
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
    private final CarrierRepository carrierRepository;
    private final AlertRepository alertRepository;
    private final OrderStatusAuditRepository auditRepository;
    private final OrderStatusEventPublisher eventPublisher;

    @Override
    @Transactional
    public Order update(Long orderId, Long carrierId, FinalStatus status) {
        log.info("Updating order status: orderId={}, carrierId={}, status={}", orderId, carrierId, status);

        carrierRepository.findById(carrierId)
                .orElseThrow(() -> new CarrierNotFoundException(carrierId));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

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
}
