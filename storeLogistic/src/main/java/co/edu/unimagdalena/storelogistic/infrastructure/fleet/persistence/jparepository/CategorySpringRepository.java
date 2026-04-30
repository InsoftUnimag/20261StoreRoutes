package co.edu.unimagdalena.storelogistic.infrastructure.fleet.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.infrastructure.fleet.persistence.jpa.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CategorySpringRepository extends JpaRepository<CategoryJpaEntity, Long> {
    Optional<CategoryJpaEntity> findByType(String type);
}