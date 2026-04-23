package co.edu.unimagdalena.storelogistic.fleet.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehiculos", indexes = {
        @Index(name = "idx_vehiculos_estado",           columnList = "estado"),
        @Index(name = "idx_vehiculos_id_categoria",     columnList = "id_categoria"),
        @Index(name = "idx_vehiculos_id_transportista", columnList = "id_transportista")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vehiculo")
    private Long vehicleId;

    @Column(name = "id_categoria", nullable = false)
    private Long categoryId;

    @Column(name = "capacidad_carga", nullable = false)
    private BigDecimal loadCapacity;

    @Column(name = "estado", nullable = false)
    private String status;

    @Column(name = "id_transportista", nullable = false)
    private Long transporterId;

    @Column(name = "peso_actual")
    private BigDecimal currentWeight;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}