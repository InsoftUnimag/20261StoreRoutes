package co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.out;

import co.edu.unimagdalena.storelogistic.domain.orderstatus.models.Carrier;

import java.util.Optional;

public interface CarrierRepository {

    Optional<Carrier> findById(Long carrierId);
}
