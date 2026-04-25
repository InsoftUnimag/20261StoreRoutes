package co.edu.unimagdalena.storelogistic.orderstatus.testdata;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Carrier;

public final class CarrierFixture {

    private CarrierFixture() {}

    public static Carrier standard() {
        return new Carrier(50L);
    }

    public static Carrier withId(Long carrierId) {
        return new Carrier(carrierId);
    }
}
