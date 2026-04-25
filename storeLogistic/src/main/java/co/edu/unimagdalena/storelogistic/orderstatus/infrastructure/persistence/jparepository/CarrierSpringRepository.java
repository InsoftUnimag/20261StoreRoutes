package co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jparepository;

import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jpa.CarrierJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarrierSpringRepository extends JpaRepository<CarrierJpaEntity, Long> {

    Optional<CarrierJpaEntity> findByTransporterId(Long transporterId);
}
