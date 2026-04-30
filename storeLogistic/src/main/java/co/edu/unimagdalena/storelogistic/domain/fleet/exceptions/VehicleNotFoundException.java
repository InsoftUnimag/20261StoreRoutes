package co.edu.unimagdalena.storelogistic.domain.fleet.exceptions;

public class VehicleNotFoundException extends LogisticsException {
    public VehicleNotFoundException(Long vehicleId) {
        super("Vehicle not found with id: " + vehicleId);
    }
}