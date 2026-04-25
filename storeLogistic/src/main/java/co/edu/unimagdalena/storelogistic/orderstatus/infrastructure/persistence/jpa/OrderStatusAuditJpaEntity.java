package co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "OrderStatusAuditEntity")
@Table(name = "order_status_audit")
public class OrderStatusAuditJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long auditId;

    @Column(name = "id_pedido", nullable = false)
    private Long orderId;

    @Column(name = "estado_anterior")
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false)
    private String estadoNuevo;

    @Column(name = "id_transportista", nullable = false)
    private Long carrierId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
