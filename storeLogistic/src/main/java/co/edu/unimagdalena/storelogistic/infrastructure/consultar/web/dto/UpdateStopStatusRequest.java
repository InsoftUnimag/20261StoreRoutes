package co.edu.unimagdalena.storelogistic.infrastructure.consultar.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class UpdateStopStatusRequest {

    @NotNull(message = "El resultado es requerido: DELIVERED o REJECTED")
    private String resultado;

    private LocalDate fechaEntrega;
}
