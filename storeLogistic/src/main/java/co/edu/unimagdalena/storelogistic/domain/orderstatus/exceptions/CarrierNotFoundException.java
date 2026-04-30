package co.edu.unimagdalena.storelogistic.domain.orderstatus.exceptions;

public class CarrierNotFoundException extends LogisticsException {

    public CarrierNotFoundException(Long carrierId) {
        super("No existe un transportista con el id proporcionado: " + carrierId);
    }
}
