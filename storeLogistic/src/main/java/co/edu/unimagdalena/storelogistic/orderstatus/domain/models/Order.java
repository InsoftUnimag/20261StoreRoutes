package co.edu.unimagdalena.storelogistic.orderstatus.domain.models;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.EffectivenessRate;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;

import java.time.LocalDateTime;
import java.util.Objects;

public class Order {

    private final Long orderId;
    private final Long clientId;
    private Long carrierId;
    private FinalStatus finalStatus;
    private EffectivenessRate effectivenessRate;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Order(Long orderId, Long clientId, Long carrierId,
                  FinalStatus finalStatus, EffectivenessRate effectivenessRate,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.orderId = Objects.requireNonNull(orderId);
        this.clientId = clientId;
        this.carrierId = carrierId;
        this.finalStatus = finalStatus;
        this.effectivenessRate = effectivenessRate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Order create(Long orderId, Long clientId, Long carrierId) {
        return new Order(orderId, clientId, carrierId, null, null, LocalDateTime.now(), null);
    }

    public static Order restore(Long orderId, Long clientId, Long carrierId,
                                FinalStatus finalStatus, EffectivenessRate effectivenessRate,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Order(orderId, clientId, carrierId, finalStatus, effectivenessRate, createdAt, updatedAt);
    }

    public void updateStatus(FinalStatus newStatus, Long newCarrierId) {
        this.finalStatus = Objects.requireNonNull(newStatus);
        this.carrierId = Objects.requireNonNull(newCarrierId);
        this.effectivenessRate = newStatus.effectivenessRate();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean requiresAlert() {
        return finalStatus == FinalStatus.NO_ENTREGADO
                || finalStatus == FinalStatus.RECHAZO_PARCIAL
                || finalStatus == FinalStatus.FALTANTE_INVENTARIO
                || finalStatus == FinalStatus.DEVOLUCION_ERROR_EMPRESA;
    }

    public Long orderId()                    { return orderId; }
    public Long clientId()                   { return clientId; }
    public Long carrierId()                  { return carrierId; }
    public FinalStatus finalStatus()         { return finalStatus; }
    public EffectivenessRate effectivenessRate() { return effectivenessRate; }
    public LocalDateTime createdAt()         { return createdAt; }
    public LocalDateTime updatedAt()         { return updatedAt; }
}
