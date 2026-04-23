package co.edu.unimagdalena.storelogistic.fleet.domain.values;

import java.math.BigDecimal;

public enum CategoryType {
    CAMIONETA_URBANA(BigDecimal.valueOf(1500)),
    CAMION_SENCILLO(BigDecimal.valueOf(5000)),
    TRACTOCAMION_REGIONAL(BigDecimal.valueOf(30000));

    private final BigDecimal maxCapacityKg;

    CategoryType(BigDecimal maxCapacityKg) {
        this.maxCapacityKg = maxCapacityKg;
    }

    public BigDecimal getMaxCapacityKg() {
        return maxCapacityKg;
    }
}