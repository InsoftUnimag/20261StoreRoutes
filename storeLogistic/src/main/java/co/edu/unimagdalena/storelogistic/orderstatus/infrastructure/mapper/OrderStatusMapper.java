package co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.mapper;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Alert;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Order;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.OrderStatusAudit;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.AlertStatus;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.AlertType;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.EffectivenessRate;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jpa.OrderAlertJpaEntity;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jpa.OrderStatusAuditJpaEntity;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jpa.OrderStatusJpaEntity;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.web.dto.UpdateOrderStatusResponse;
import org.mapstruct.Mapper;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public abstract class OrderStatusMapper {

    public UpdateOrderStatusResponse toResponse(Order order) {
        if (order == null) return null;
        return UpdateOrderStatusResponse.builder()
                .idPedido(order.orderId())
                .estadoFinal(order.finalStatus() != null ? order.finalStatus().displayName() : null)
                .tasaEfectividad(order.effectivenessRate() != null ? order.effectivenessRate().value() : null)
                .idTransportista(order.carrierId())
                .fechaActualizacion(order.updatedAt())
                .build();
    }

    public OrderStatusJpaEntity toEntity(Order order) {
        if (order == null) return null;
        return OrderStatusJpaEntity.builder()
                .orderId(order.orderId())
                .clientId(order.clientId())
                .carrierId(order.carrierId())
                .estadoFinal(order.finalStatus() != null ? order.finalStatus().displayName() : null)
                .tasaEfectividad(order.effectivenessRate() != null ? order.effectivenessRate().value() : null)
                .updatedAt(order.updatedAt())
                .createdAt(order.createdAt())
                .build();
    }

    public Order toDomain(OrderStatusJpaEntity entity) {
        if (entity == null) return null;
        FinalStatus finalStatus = null;
        EffectivenessRate effectivenessRate = null;
        if (entity.getEstadoFinal() != null) {
            finalStatus = FinalStatus.fromDisplayName(entity.getEstadoFinal());
            effectivenessRate = finalStatus.effectivenessRate();
        }
        return Order.restore(
                entity.getOrderId(),
                entity.getClientId(),
                entity.getCarrierId(),
                finalStatus,
                effectivenessRate,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public OrderAlertJpaEntity toAlertEntity(Alert alert) {
        if (alert == null) return null;
        return OrderAlertJpaEntity.builder()
                .alertId(alert.alertId())
                .orderId(alert.orderId())
                .carrierId(alert.carrierId())
                .estadoFinalRegistrado(alert.registeredStatus() != null ? alert.registeredStatus().displayName() : null)
                .tipo(alert.type() != null ? alert.type().name() : null)
                .descripcion(alert.description())
                .estado(alert.status() != null ? alert.status().name() : AlertStatus.PENDIENTE.name())
                .asignadoASupervisor(alert.assignedToSupervisor())
                .createdAt(alert.createdAt() != null ? alert.createdAt() : LocalDateTime.now())
                .build();
    }

    public Alert toAlertDomain(OrderAlertJpaEntity entity) {
        if (entity == null) return null;
        FinalStatus registeredStatus = entity.getEstadoFinalRegistrado() != null
                ? FinalStatus.fromDisplayName(entity.getEstadoFinalRegistrado()) : null;
        AlertType type = entity.getTipo() != null ? AlertType.valueOf(entity.getTipo()) : null;
        AlertStatus status = entity.getEstado() != null ? AlertStatus.valueOf(entity.getEstado()) : AlertStatus.PENDIENTE;
        return new Alert(
                entity.getAlertId(),
                entity.getOrderId(),
                entity.getCarrierId(),
                registeredStatus,
                type,
                entity.getDescripcion(),
                status,
                entity.isAsignadoASupervisor(),
                entity.getCreatedAt()
        );
    }

    public OrderStatusAuditJpaEntity toAuditEntity(OrderStatusAudit audit) {
        if (audit == null) return null;
        return OrderStatusAuditJpaEntity.builder()
                .auditId(audit.auditId())
                .orderId(audit.orderId())
                .estadoAnterior(audit.previousStatus() != null ? audit.previousStatus().displayName() : null)
                .estadoNuevo(audit.newStatus().displayName())
                .carrierId(audit.carrierId())
                .timestamp(audit.timestamp() != null ? audit.timestamp() : LocalDateTime.now())
                .build();
    }

    public OrderStatusAudit toAuditDomain(OrderStatusAuditJpaEntity entity) {
        if (entity == null) return null;
        FinalStatus previousStatus = entity.getEstadoAnterior() != null
                ? FinalStatus.fromDisplayName(entity.getEstadoAnterior()) : null;
        FinalStatus newStatus = FinalStatus.fromDisplayName(entity.getEstadoNuevo());
        return new OrderStatusAudit(
                entity.getAuditId(),
                entity.getOrderId(),
                previousStatus,
                newStatus,
                entity.getCarrierId(),
                entity.getTimestamp()
        );
    }
}
