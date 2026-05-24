package co.edu.unimagdalena.storelogistic.domain.route.models;

import co.edu.unimagdalena.storelogistic.domain.route.values.LogisticWeight;

import java.util.Objects;

public class Order {

    private final Long orderId;
    private final Long clientId;
    private final LogisticWeight logisticWeight;
    private final String deliveryAddress;

    public Order(Long orderId, Long clientId, LogisticWeight logisticWeight, String deliveryAddress) {
        this.orderId         = Objects.requireNonNull(orderId);
        this.clientId        = Objects.requireNonNull(clientId);
        this.logisticWeight  = Objects.requireNonNull(logisticWeight);
        this.deliveryAddress = Objects.requireNonNull(deliveryAddress);
    }

    public Long orderId()              { return orderId; }
    public Long clientId()             { return clientId; }
    public LogisticWeight logisticWeight() { return logisticWeight; }
    public String deliveryAddress()    { return deliveryAddress; }
}