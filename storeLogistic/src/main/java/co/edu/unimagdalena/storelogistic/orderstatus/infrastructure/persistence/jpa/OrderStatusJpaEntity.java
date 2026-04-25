package co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "OrderStatusEntity")
@Table(name = "orders")
public class OrderStatusJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_order")
    private Long orderId;

    @Column(name = "logistic_weight", nullable = false)
    private BigDecimal logisticWeight;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "id_cliente")
    private Long clientId;

    @Column(name = "id_transportista")
    private Long carrierId;

    @Column(name = "estado_final")
    private String estadoFinal;

    @Column(name = "tasa_efectividad")
    private Integer tasaEfectividad;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
