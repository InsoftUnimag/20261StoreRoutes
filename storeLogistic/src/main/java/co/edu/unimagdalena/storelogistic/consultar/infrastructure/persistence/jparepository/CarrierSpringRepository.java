package co.edu.unimagdalena.storelogistic.consultar.infrastructure.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.consultar.infrastructure.persistence.jpa.CarrierJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarrierSpringRepository extends JpaRepository<CarrierJpaEntity, Long> {
}
