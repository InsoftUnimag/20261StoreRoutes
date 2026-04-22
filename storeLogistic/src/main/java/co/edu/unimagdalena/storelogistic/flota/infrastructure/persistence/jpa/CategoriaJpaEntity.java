package co.edu.unimagdalena.storelogistic.flota.infrastructure.persistence.jpa;

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
public class CategoriaJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long idCategoria;

    @Column(name = "tipo", nullable = false, unique = true)
    private String tipo;

    @Column(name = "capacidad_maxima_kg", nullable = false)
    private BigDecimal capacidadMaximaKg;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}


