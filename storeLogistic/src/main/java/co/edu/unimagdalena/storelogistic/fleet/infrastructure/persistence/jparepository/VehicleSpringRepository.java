package co.edu.unimagdalena.storelogistic.fleet.infrastructure.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.fleet.infrastructure.persistence.jpa.VehicleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface VehicleSpringRepository extends JpaRepository<VehicleJpaEntity, Long> {

    @Query("""
    SELECT v FROM VehicleJpaEntity v
    WHERE (:categoryId IS NULL OR v.categoryId = :categoryId)
    AND (:status IS NULL OR v.status = :status)
    AND (:minCapacity IS NULL OR v.loadCapacity >= :minCapacity)
    AND (:maxCapacity IS NULL OR v.loadCapacity <= :maxCapacity)
    ORDER BY v.vehicleId
    """)
    List<VehicleJpaEntity> findWithFilters(
            @Param("categoryId") Long categoryId,
            @Param("status") String status,
            @Param("minCapacity") BigDecimal minCapacity,
            @Param("maxCapacity") BigDecimal maxCapacity
    );

    List<VehicleJpaEntity> findByStatus(String status);
}