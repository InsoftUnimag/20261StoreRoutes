package co.edu.unimagdalena.storelogistic.consultar.testdata;

import co.edu.unimagdalena.storelogistic.route.domain.models.Stop;
import co.edu.unimagdalena.storelogistic.route.domain.values.StopStatus;

public class StopFixture {

    public static Stop standard() {
        return Stop.reconstitute(1L, 1L, 10L, 1, "Calle 123 #45-67", StopStatus.PENDING, null, "3001234567");
    }

    public static Stop withSequence(int sequence) {
        return Stop.reconstitute((long) sequence, 1L, (long) (10 + sequence), sequence,
                "Calle " + sequence, StopStatus.PENDING, null, "300000000" + sequence);
    }

    public static Stop withRoute(Long routeId, int sequence) {
        return Stop.reconstitute((long) sequence, routeId, (long) (10 + sequence), sequence,
                "Dirección " + sequence, StopStatus.PENDING, null, null);
    }
}
