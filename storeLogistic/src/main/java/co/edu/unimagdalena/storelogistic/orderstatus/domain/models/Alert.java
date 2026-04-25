package co.edu.unimagdalena.storelogistic.orderstatus.domain.models;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.AlertStatus;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.AlertType;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;

import java.time.LocalDateTime;

public class Alert {

    private final Long alertId;
    private final Long orderId;
    private final Long carrierId;
    private final FinalStatus registeredStatus;
    private final AlertType type;
    private final String description;
    private final AlertStatus status;
    private final boolean assignedToSupervisor;
    private final LocalDateTime createdAt;

    public Alert(Long alertId, Long orderId, Long carrierId,
                 FinalStatus registeredStatus, AlertType type, String description,
                 AlertStatus status, boolean assignedToSupervisor, LocalDateTime createdAt) {
        this.alertId = alertId;
        this.orderId = orderId;
        this.carrierId = carrierId;
        this.registeredStatus = registeredStatus;
        this.type = type;
        this.description = description;
        this.status = status;
        this.assignedToSupervisor = assignedToSupervisor;
        this.createdAt = createdAt;
    }

    public static Alert createFor(Order order) {
        FinalStatus registeredStatus = order.finalStatus();
        AlertType type = switch (registeredStatus) {
            case NO_ENTREGADO -> AlertType.NO_ENTREGADO;
            case RECHAZO_PARCIAL -> AlertType.RECHAZO;
            case FALTANTE_INVENTARIO -> AlertType.FALTANTE;
            case DEVOLUCION_ERROR_EMPRESA -> AlertType.DEVOLUCION;
            default -> throw new IllegalArgumentException(
                    "Estado " + registeredStatus + " no genera alerta");
        };
        String description = switch (registeredStatus) {
            case NO_ENTREGADO -> "Pedido no entregado por el transportista";
            case RECHAZO_PARCIAL -> "Rechazo parcial por parte del cliente";
            case FALTANTE_INVENTARIO -> "Faltante de inventario detectado en el pedido";
            case DEVOLUCION_ERROR_EMPRESA -> "Devolución por error de la empresa";
            default -> "";
        };
        return new Alert(null, order.orderId(), order.carrierId(),
                registeredStatus, type, description,
                AlertStatus.PENDIENTE, true, LocalDateTime.now());
    }

    public Long alertId()                   { return alertId; }
    public Long orderId()                   { return orderId; }
    public Long carrierId()                 { return carrierId; }
    public FinalStatus registeredStatus()   { return registeredStatus; }
    public AlertType type()                 { return type; }
    public String description()             { return description; }
    public AlertStatus status()             { return status; }
    public boolean assignedToSupervisor()   { return assignedToSupervisor; }
    public LocalDateTime createdAt()        { return createdAt; }
}
