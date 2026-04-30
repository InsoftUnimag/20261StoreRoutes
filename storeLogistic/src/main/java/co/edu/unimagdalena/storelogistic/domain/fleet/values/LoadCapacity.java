package co.edu.unimagdalena.storelogistic.domain.fleet.values;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public class LoadCapacity {
    private final BigDecimal weightKg;

    public LoadCapacity(BigDecimal weightKg) {
        this.weightKg = Optional.ofNullable(weightKg)
                .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
                .orElseThrow(() -> new IllegalArgumentException("Load capacity must be greater than 0"));
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoadCapacity that = (LoadCapacity) o;
        return Objects.equals(weightKg, that.weightKg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(weightKg);
    }

    @Override
    public String toString() {
        return weightKg + " kg";
    }
}