package co.edu.unimagdalena.storelogistic.route.domain.models;

import co.edu.unimagdalena.storelogistic.route.domain.values.RouteCapacity;
import co.edu.unimagdalena.storelogistic.route.domain.values.VehicleType;

public class RouteVehicle {

    private final Long vehicleId;
    private final VehicleType type;
    private final RouteCapacity capacity;

    public RouteVehicle(Long vehicleId, VehicleType type, RouteCapacity capacity) {
        this.vehicleId = vehicleId;
        this.type      = type;
        this.capacity  = capacity;
    }

    public Long vehicleId()       { return vehicleId; }
    public VehicleType type()     { return type; }
    public RouteCapacity capacity() { return capacity; }
}