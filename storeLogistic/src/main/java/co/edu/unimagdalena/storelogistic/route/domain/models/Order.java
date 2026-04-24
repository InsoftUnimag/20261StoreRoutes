package co.edu.unimagdalena.storelogistic.route.domain.models;

import co.edu.unimagdalena.storelogistic.route.domain.values.LogisticWeight;

import java.util.Objects;

public class Order {

    private final Long orderId;
    private final LogisticWeight logisticWeight;
    private final String deliveryAddress;

    public Order(Long orderId, LogisticWeight logisticWeight, String deliveryAddress) {
        this.orderId         = Objects.requireNonNull(orderId);
        this.logisticWeight  = Objects.requireNonNull(logisticWeight);
        this.deliveryAddress = Objects.requireNonNull(deliveryAddress);
    }

    public Long orderId()              { return orderId; }
    public LogisticWeight logisticWeight() { return logisticWeight; }
    public String deliveryAddress()    { return deliveryAddress; }
}