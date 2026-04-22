package co.edu.unimagdalena.storelogistic.flota.infrastructure.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.flota.infrastructure.persistence.jpa.CategoriaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CategoriaSpringRepository extends JpaRepository<CategoriaJpaEntity, Long> {
    Optional<CategoriaJpaEntity> findByTipo(String tipo);
}

