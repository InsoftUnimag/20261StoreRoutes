package co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jpa.RouteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface RouteSpringRepository extends JpaRepository<RouteJpaEntity, Long> {

    @Query(value = """
            SELECT * FROM routes
            WHERE status = 'AVAILABLE'
              AND (total_capacity_kg - accumulated_weight_kg) >= :weightKg
            ORDER BY (accumulated_weight_kg / total_capacity_kg) DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<RouteJpaEntity> findBestAvailableWithCapacity(@Param("weightKg") BigDecimal weightKg);
}
