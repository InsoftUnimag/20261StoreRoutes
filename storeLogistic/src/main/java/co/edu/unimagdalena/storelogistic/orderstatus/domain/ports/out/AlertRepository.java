package co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Alert;

public interface AlertRepository {

    Alert save(Alert alert);
}
