package co.edu.unimagdalena.storelogistic.route.domain.values;

import java.math.BigDecimal;
import java.util.Objects;

public record LogisticWeight(BigDecimal valueKg) {

    public LogisticWeight {
        Objects.requireNonNull(valueKg, "Logistic weight must not be null");
        if (valueKg.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Logistic weight must be greater than zero, got: " + valueKg);
    }

    public LogisticWeight add(LogisticWeight other) {
        return new LogisticWeight(this.valueKg.add(other.valueKg));
    }

    public static LogisticWeight of(BigDecimal kg) {
        return new LogisticWeight(kg);
    }

    public static LogisticWeight of(double kg) {
        return new LogisticWeight(BigDecimal.valueOf(kg));
    }
}