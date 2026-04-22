package co.edu.unimagdalena.storelogistic.flota.infrastructure.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.flota.infrastructure.persistence.jpa.VehiculoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface VehiculoSpringRepository extends JpaRepository<VehiculoJpaEntity, Long> {

    @Query("""
    SELECT v FROM VehiculoJpaEntity v 
    WHERE (:categoriaid IS NULL OR v.idCategoria = :categoriaid)
    AND (:estado IS NULL OR v.estado = :estado)
    AND (:capacidadMin IS NULL OR v.capacidadCarga >= :capacidadMin)
    AND (:capacidadMax IS NULL OR v.capacidadCarga <= :capacidadMax)
    ORDER BY v.idVehiculo
""")
    List<VehiculoJpaEntity> findWithFilters(
            @Param("categoriaid") Long categoriaId,
            @Param("estado") String estado,
            @Param("capacidadMin") BigDecimal capacidadMin,
            @Param("capacidadMax") BigDecimal capacidadMax
    );

    List<VehiculoJpaEntity> findByEstado(String estado);
}

