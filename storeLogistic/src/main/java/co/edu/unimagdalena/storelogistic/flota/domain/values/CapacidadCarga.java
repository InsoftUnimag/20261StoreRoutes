package co.edu.unimagdalena.storelogistic.flota.domain.values;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public class CapacidadCarga {
    private final BigDecimal pesoKg;

    public CapacidadCarga(BigDecimal pesoKg) {
        this.pesoKg = Optional.ofNullable(pesoKg)
                .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
                .orElseThrow(() -> new IllegalArgumentException("La capacidad de carga debe ser mayor a 0"));
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CapacidadCarga that = (CapacidadCarga) o;
        return Objects.equals(pesoKg, that.pesoKg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pesoKg);
    }

    @Override
    public String toString() {
        return pesoKg + " kg";
    }
}

