package co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.jpa.RouteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
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

    @Query("SELECT r FROM RouteJpaEntity r WHERE r.status = :status ORDER BY r.routeId ASC")
    List<RouteJpaEntity> findByStatus(@Param("status") String status);
}
