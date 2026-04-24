package co.edu.unimagdalena.storelogistic.consultar.infrastructure.persistence.repository;

import co.edu.unimagdalena.storelogistic.consultar.domain.models.Carrier;
import co.edu.unimagdalena.storelogistic.consultar.domain.ports.out.CarrierRepository;
import co.edu.unimagdalena.storelogistic.consultar.infrastructure.persistence.jpa.CarrierJpaEntity;
import co.edu.unimagdalena.storelogistic.consultar.infrastructure.persistence.jparepository.CarrierSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CarrierRepositoryAdapter implements CarrierRepository {

    private final CarrierSpringRepository springRepository;

    @Override
    public Optional<Carrier> findById(Long carrierId) {
        return springRepository.findById(carrierId).map(this::toDomain);
    }

    private Carrier toDomain(CarrierJpaEntity e) {
        return new Carrier(e.getCarrierId(), e.getName(), e.getStatus(), e.getEmail());
    }
}
