package co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.repository;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Carrier;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out.CarrierRepository;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jparepository.CarrierSpringRepository;
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
