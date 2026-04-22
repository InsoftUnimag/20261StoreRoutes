package co.edu.unimagdalena.storelogistic.flota.domain.values;

import java.math.BigDecimal;

public enum TipoCategoria {
    CAMIONETA_URBANA(BigDecimal.valueOf(1500)),
    CAMION_SENCILLO(BigDecimal.valueOf(5000)),
    TRACTOCAMION_REGIONAL(BigDecimal.valueOf(30000));

    private final BigDecimal capacidadMaximaKg;

    TipoCategoria(BigDecimal capacidadMaximaKg) {
        this.capacidadMaximaKg = capacidadMaximaKg;
    }

    public BigDecimal getCapacidadMaximaKg() {
        return capacidadMaximaKg;
    }
}

