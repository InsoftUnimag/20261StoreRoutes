package co.edu.unimagdalena.storelogistic.fleet.domain.exceptions;

public class VehicleNotFoundException extends LogisticsException {
    public VehicleNotFoundException(Long vehicleId) {
        super("Vehicle not found with id: " + vehicleId);
    }
}