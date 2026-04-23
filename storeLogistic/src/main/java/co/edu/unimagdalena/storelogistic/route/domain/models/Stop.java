package co.edu.unimagdalena.storelogistic.route.domain.models;

import co.edu.unimagdalena.storelogistic.route.domain.values.StopStatus;

import java.time.LocalDate;

public class Stop {

    private Long stopId;
    private Long routeId;
    private Long orderId;
    private int sequence;
    private String deliveryAddress;
    private StopStatus status;
    private LocalDate deliveryDate;

    private Stop() {}

    public static Stop create(Long routeId, Long orderId, int sequence, String deliveryAddress) {
        Stop stop = new Stop();
        stop.routeId         = routeId;
        stop.orderId         = orderId;
        stop.sequence        = sequence;
        stop.deliveryAddress = deliveryAddress;
        stop.status          = StopStatus.PENDING;
        return stop;
    }

    public static Stop reconstitute(Long stopId, Long routeId, Long orderId, int sequence,
                                    String deliveryAddress, StopStatus status, LocalDate deliveryDate) {
        Stop stop = new Stop();
        stop.stopId          = stopId;
        stop.routeId         = routeId;
        stop.orderId         = orderId;
        stop.sequence        = sequence;
        stop.deliveryAddress = deliveryAddress;
        stop.status          = status;
        stop.deliveryDate    = deliveryDate;
        return stop;
    }

    public void markDelivered(LocalDate date) {
        this.status       = StopStatus.DELIVERED;
        this.deliveryDate = date;
    }

    public void markRejected() {
        this.status = StopStatus.REJECTED;
    }

    public Long stopId()           { return stopId; }
    public Long routeId()          { return routeId; }
    public Long orderId()          { return orderId; }
    public int sequence()          { return sequence; }
    public String deliveryAddress(){ return deliveryAddress; }
    public StopStatus status()     { return status; }
    public LocalDate deliveryDate(){ return deliveryDate; }

    public void setStopId(Long stopId) { this.stopId = stopId; }
}