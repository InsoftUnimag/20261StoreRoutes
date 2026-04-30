package co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.jpa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "categorias")
public class RouteCategoryJpaEntity {

    @Id
    @Column(name = "id_categoria")
    private Long id;

    @Column(name = "tipo")
    private String tipo;

    @Column(name = "capacidad_maxima_kg")
    private BigDecimal maxCapacityKg;
}