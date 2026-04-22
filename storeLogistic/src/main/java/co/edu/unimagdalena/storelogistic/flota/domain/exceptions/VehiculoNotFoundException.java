package co.edu.unimagdalena.storelogistic.flota.domain.exceptions;

public class VehiculoNotFoundException extends FlotaException {
    public VehiculoNotFoundException(Long idVehiculo) {
        super("Vehículo no encontrado con id: " + idVehiculo);
    }
}

