package co.edu.unimagdalena.storelogistic.consultar.domain.ports.out;

import co.edu.unimagdalena.storelogistic.consultar.domain.models.Carrier;

import java.util.Optional;

public interface CarrierRepository {

    Optional<Carrier> findById(Long carrierId);
}
