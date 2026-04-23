package co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "vehiculos")
public class RouteVehicleJpaEntity {

    @Id
    @Column(name = "id_vehiculo")
    private Long vehicleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria")
    private RouteCategoryJpaEntity category;

    @Column(name = "capacidad_carga")
    private BigDecimal loadCapacityKg;

    @Column(name = "estado")
    private String status;
}
