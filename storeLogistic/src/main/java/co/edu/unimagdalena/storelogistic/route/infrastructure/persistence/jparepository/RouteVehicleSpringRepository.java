package co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jpa.RouteVehicleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RouteVehicleSpringRepository extends JpaRepository<RouteVehicleJpaEntity, Long> {

    @Query("SELECT v FROM RouteVehicleJpaEntity v WHERE v.category.tipo = :categoryName AND v.status = 'DISPONIBLE'")
    Optional<RouteVehicleJpaEntity> findFirstAvailableByCategory(@Param("categoryName") String categoryName);

    @Query("SELECT v FROM RouteVehicleJpaEntity v WHERE v.status = 'DISPONIBLE' ORDER BY v.loadCapacityKg DESC LIMIT 1")
    Optional<RouteVehicleJpaEntity> findTopAvailableByMaxCapacity();
}
