package co.edu.unimagdalena.storelogistic.infrastructure.fleet.persistence.jpa;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long categoryId;

    @Column(name = "tipo", nullable = false, unique = true)
    private String type;

    @Column(name = "capacidad_maxima_kg", nullable = false)
    private BigDecimal maxCapacityKg;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}