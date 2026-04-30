package co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.jpa;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "routes")
public class RouteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_route")
    private Long routeId;

    @Column(name = "id_vehicle")
    private Long vehicleId;

    @Column(name = "total_capacity_kg", nullable = false)
    private BigDecimal totalCapacityKg;

    @Column(name = "accumulated_weight_kg", nullable = false)
    private BigDecimal accumulatedWeightKg;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "dispatch_date", nullable = false)
    private LocalDate dispatchDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_route", insertable = false, updatable = false)
    @Builder.Default
    private List<StopJpaEntity> stops = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
