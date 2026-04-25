package co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Carrier;

import java.util.Optional;

public interface CarrierRepository {

    Optional<Carrier> findById(Long carrierId);
}
