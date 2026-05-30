package co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotBlank(message = "El campo estadoFinal es obligatorio")
    private String estadoFinal;
}
