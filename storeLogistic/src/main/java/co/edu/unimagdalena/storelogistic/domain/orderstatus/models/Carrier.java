package co.edu.unimagdalena.storelogistic.domain.orderstatus.models;

import java.util.Objects;

public class Carrier {

    private final Long carrierId;

    public Carrier(Long carrierId) {
        this.carrierId = Objects.requireNonNull(carrierId);
    }

    public Long carrierId() {
        return carrierId;
    }
}
