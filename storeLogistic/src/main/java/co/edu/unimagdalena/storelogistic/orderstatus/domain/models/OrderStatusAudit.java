package co.edu.unimagdalena.storelogistic.orderstatus.domain.models;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;

import java.time.LocalDateTime;

public class OrderStatusAudit {

    private final Long auditId;
    private final Long orderId;
    private final FinalStatus previousStatus;
    private final FinalStatus newStatus;
    private final Long carrierId;
    private final LocalDateTime timestamp;

    public OrderStatusAudit(Long auditId, Long orderId,
                            FinalStatus previousStatus, FinalStatus newStatus,
                            Long carrierId, LocalDateTime timestamp) {
        this.auditId = auditId;
        this.orderId = orderId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.carrierId = carrierId;
        this.timestamp = timestamp;
    }

    public Long auditId()             { return auditId; }
    public Long orderId()             { return orderId; }
    public FinalStatus previousStatus() { return previousStatus; }
    public FinalStatus newStatus()    { return newStatus; }
    public Long carrierId()           { return carrierId; }
    public LocalDateTime timestamp()  { return timestamp; }
}
