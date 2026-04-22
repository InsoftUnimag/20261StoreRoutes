package co.edu.unimagdalena.storelogistic.flota.domain.values;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EstadoVehiculoTest {

    @Test
    void testTransicionValidaEnMantenimientoADisponible() {
        assertTrue(EstadoVehiculo.EN_MANTENIMIENTO.esTransicionValida(EstadoVehiculo.DISPONIBLE));
    }

    @Test
    void testTransicionValidaDisponibleAEnRuta() {
        assertTrue(EstadoVehiculo.DISPONIBLE.esTransicionValida(EstadoVehiculo.EN_RUTA));
    }

    @Test
    void testTransicionValidaEnRutaADisponible() {
        assertTrue(EstadoVehiculo.EN_RUTA.esTransicionValida(EstadoVehiculo.DISPONIBLE));
    }

    @Test
    void testTransicionAFueraDeServicioDesdeQualquier() {
        assertTrue(EstadoVehiculo.EN_MANTENIMIENTO.esTransicionValida(EstadoVehiculo.FUERA_DE_SERVICIO));
        assertTrue(EstadoVehiculo.DISPONIBLE.esTransicionValida(EstadoVehiculo.FUERA_DE_SERVICIO));
        assertTrue(EstadoVehiculo.EN_RUTA.esTransicionValida(EstadoVehiculo.FUERA_DE_SERVICIO));
    }

    @Test
    void testTransicionValidaFueraDeServicioAEnMantenimiento() {
        assertTrue(EstadoVehiculo.FUERA_DE_SERVICIO.esTransicionValida(EstadoVehiculo.EN_MANTENIMIENTO));
    }

    @Test
    void testTransicionInvalidaAlMismoEstado() {
        assertFalse(EstadoVehiculo.EN_MANTENIMIENTO.esTransicionValida(EstadoVehiculo.EN_MANTENIMIENTO));
        assertFalse(EstadoVehiculo.DISPONIBLE.esTransicionValida(EstadoVehiculo.DISPONIBLE));
    }

    @Test
    void testTransicionInvalidaEnMantenimientoAEnRuta() {
        assertFalse(EstadoVehiculo.EN_MANTENIMIENTO.esTransicionValida(EstadoVehiculo.EN_RUTA));
    }

    @Test
    void testTransicionInvalidaFueraDeServicioADisponible() {
        assertFalse(EstadoVehiculo.FUERA_DE_SERVICIO.esTransicionValida(EstadoVehiculo.DISPONIBLE));
    }

    @Test
    void testMensajeTransicionInvalida() {
        String mensaje = EstadoVehiculo.EN_MANTENIMIENTO.obtenerMensajeTransicionInvalida(EstadoVehiculo.EN_RUTA);
        assertNotNull(mensaje);
        assertTrue(mensaje.contains("EN_MANTENIMIENTO"));
        assertTrue(mensaje.contains("EN_RUTA"));
    }
}

