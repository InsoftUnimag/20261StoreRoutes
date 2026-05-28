package co.edu.unimagdalena.storelogistic.infrastructure.fleet.client;

import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.InvalidTransporterException;
import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.LogisticsException;
import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.TransporterNotAvailableException;
import co.edu.unimagdalena.storelogistic.domain.fleet.ports.out.TransporterServicePort;
import co.edu.unimagdalena.storelogistic.infrastructure.fleet.web.dto.TransporterAvailableClientDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@Component
public class TransporterServiceClient implements TransporterServicePort {

    private final @Qualifier("transporterRestClient") RestClient restClient;
    private final @Qualifier("transporterRetryTemplate") RetryTemplate retryTemplate;

    public TransporterServiceClient(
            @Qualifier("transporterRestClient") RestClient restClient,
            @Qualifier("transporterRetryTemplate") RetryTemplate retryTemplate) {
        this.restClient = restClient;
        this.retryTemplate = retryTemplate;
    }

    @Override
    public Long getAvailable() {
        log.info("Requesting available transporter from external module");
        return retryTemplate.execute(
                context -> {
                    logAttempt(context.getRetryCount());
                    return doGetAvailable();
                },
                context -> {
                    log.error("Transporter module unavailable after {} attempts", context.getRetryCount());
                    throw new LogisticsException("El módulo de Transportista no está disponible. Intente nuevamente más tarde.");
                }
        );
    }

    @Override
    public void validateExistence(Long transporterId) {
        log.info("Validating transporter existence: id={}", transporterId);
        retryTemplate.execute(
                context -> {
                    logAttempt(context.getRetryCount());
                    doValidateExistence(transporterId);
                    return null;
                },
                context -> {
                    log.error("Transporter module unavailable after {} attempts for validation", context.getRetryCount());
                    throw new LogisticsException("El módulo de Transportista no está disponible. Intente nuevamente más tarde.");
                }
        );
    }

    private Long doGetAvailable() {
        var dto = restClient.get()
                .uri("/transportistas/disponible")
                .retrieve()
                .onStatus(status -> status == HttpStatus.NOT_FOUND, (request, response) -> {
                    throw new TransporterNotAvailableException();
                })
                .onStatus(status -> status.is4xxClientError(), (request, response) -> {
                    throw new LogisticsException(
                            "Error al consultar transportista disponible: " + response.getStatusText());
                })
                .body(TransporterAvailableClientDTO.class);

        return Optional.ofNullable(dto)
                .filter(t -> "DISPONIBLE".equals(t.getEstado()))
                .map(TransporterAvailableClientDTO::getIdTransportista)
                .orElseThrow(TransporterNotAvailableException::new);
    }

    private void doValidateExistence(Long transporterId) {
        restClient.get()
                .uri("/transportistas/{id}", transporterId)
                .retrieve()
                .onStatus(status -> status == HttpStatus.NOT_FOUND, (request, response) -> {
                    throw new InvalidTransporterException(transporterId);
                })
                .onStatus(status -> status.is4xxClientError(), (request, response) -> {
                    throw new LogisticsException(
                            "Error al validar transportista " + transporterId + ": " + response.getStatusText());
                })
                .toBodilessEntity();
    }

    @Override
    public void updateStatus(Long transporterId, String estado) {
        log.info("Updating transporter status: id={}, estado={}", transporterId, estado);
        retryTemplate.execute(
                context -> {
                    logAttempt(context.getRetryCount());
                    doUpdateStatus(transporterId, estado);
                    return null;
                },
                context -> {
                    log.error("Transporter module unavailable after {} attempts for status update", context.getRetryCount());
                    throw new LogisticsException("El módulo de Transportista no está disponible. Intente nuevamente más tarde.");
                }
        );
    }

    private void doUpdateStatus(Long transporterId, String estado) {
        restClient.patch()
                .uri("/transportistas/{id}/estado?estado={estado}", transporterId, estado)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), (request, response) -> {
                    throw new LogisticsException(
                            "Error al actualizar estado del transportista " + transporterId + ": " + response.getStatusText());
                })
                .toBodilessEntity();
    }

    private void logAttempt(int retryCount) {
        if (retryCount > 0) {
            log.warn("Retry attempt {}/3 for transporter module call", retryCount);
        }
    }
}
