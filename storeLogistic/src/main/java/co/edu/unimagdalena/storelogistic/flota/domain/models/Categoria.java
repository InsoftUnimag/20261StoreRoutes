package co.edu.unimagdalena.storelogistic.flota.domain.models;

import co.edu.unimagdalena.storelogistic.flota.domain.values.TipoCategoria;
import co.edu.unimagdalena.storelogistic.flota.domain.values.CapacidadCarga;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {
    private Long idCategoria;
    private TipoCategoria tipo;
    private CapacidadCarga capacidadMaxima;
}

