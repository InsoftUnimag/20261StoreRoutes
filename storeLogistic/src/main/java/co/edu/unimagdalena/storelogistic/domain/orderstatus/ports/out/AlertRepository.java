package co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.out;

import co.edu.unimagdalena.storelogistic.domain.orderstatus.models.Alert;

public interface AlertRepository {

    Alert save(Alert alert);
}
