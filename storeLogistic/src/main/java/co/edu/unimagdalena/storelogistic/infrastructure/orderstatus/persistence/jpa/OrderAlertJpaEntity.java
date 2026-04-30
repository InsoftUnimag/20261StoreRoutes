package co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jpa;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "OrderAlertEntity")
@Table(name = "order_alerts")
public class OrderAlertJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Long alertId;

    @Column(name = "id_pedido", nullable = false)
    private Long orderId;

    @Column(name = "id_transportista", nullable = false)
    private Long carrierId;

    @Column(name = "estado_final_registrado", nullable = false)
    private String estadoFinalRegistrado;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "asignado_a_supervisor", nullable = false)
    private boolean asignadoASupervisor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
