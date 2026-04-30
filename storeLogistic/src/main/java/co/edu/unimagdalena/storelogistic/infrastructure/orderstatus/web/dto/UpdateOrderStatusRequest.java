package co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotNull(message = "El campo idTransportista es obligatorio y debe ser un número entero válido")
    private Long idTransportista;

    @NotBlank(message = "El campo estadoFinal es obligatorio")
    private String estadoFinal;
}
