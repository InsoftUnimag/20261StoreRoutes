package co.edu.unimagdalena.storelogistic.domain.orderstatus.values;

import java.util.Arrays;

public enum FinalStatus {

    ENTREGADO_COMPLETO("Entregado Completo", 100),
    RECHAZO_PARCIAL("Rechazo Parcial", 80),
    NO_ENTREGADO("No Entregado", 0),
    DEVOLUCION_ERROR_EMPRESA("Devolución (Error Empresa)", 0),
    FALTANTE_INVENTARIO("Faltante de Inventario", -100);

    private final String displayName;
    private final EffectivenessRate effectivenessRate;

    FinalStatus(String displayName, int rateValue) {
        this.displayName = displayName;
        this.effectivenessRate = EffectivenessRate.of(rateValue);
    }

    public String displayName() {
        return displayName;
    }

    public EffectivenessRate effectivenessRate() {
        return effectivenessRate;
    }

    public static FinalStatus fromDisplayName(String name) {
        return Arrays.stream(values())
                .filter(s -> s.displayName.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No FinalStatus with displayName: " + name));
    }
}
