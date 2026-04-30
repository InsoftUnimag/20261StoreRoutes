package co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.repository;

import co.edu.unimagdalena.storelogistic.domain.orderstatus.models.Carrier;
import co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.out.CarrierRepository;
import co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jparepository.CarrierSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CarrierRepositoryAdapter implements CarrierRepository {

    private final CarrierSpringRepository springRepository;

    @Override
    public Optional<Carrier> findById(Long carrierId) {
        if (springRepository.existsByTransporterId(carrierId)) {
            return Optional.of(new Carrier(carrierId));
        }
        return Optional.empty();
    }
}
