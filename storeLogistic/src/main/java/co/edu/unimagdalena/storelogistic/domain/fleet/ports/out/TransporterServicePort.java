package co.edu.unimagdalena.storelogistic.domain.fleet.ports.out;

public interface TransporterServicePort {
    Long getAvailable();
    void validateExistence(Long transporterId);
    void updateStatus(Long transporterId, String estado);
}