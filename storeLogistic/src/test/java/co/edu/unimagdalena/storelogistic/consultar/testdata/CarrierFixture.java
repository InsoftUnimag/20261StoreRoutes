package co.edu.unimagdalena.storelogistic.consultar.testdata;

import co.edu.unimagdalena.storelogistic.consultar.domain.models.Carrier;

public class CarrierFixture {

    public static Carrier active() {
        return new Carrier(1L, "Juan Eguis", "ACTIVE", "juan@logistica.com");
    }

    public static Carrier inactive() {
        return new Carrier(2L, "Pedro Ruiz", "INACTIVE", "pedro@logistica.com");
    }

    public static Carrier withId(Long id) {
        return new Carrier(id, "Transportista " + id, "ACTIVE", "transportista" + id + "@logistica.com");
    }
}
