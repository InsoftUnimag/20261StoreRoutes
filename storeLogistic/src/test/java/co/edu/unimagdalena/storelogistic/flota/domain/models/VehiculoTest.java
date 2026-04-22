package co.edu.unimagdalena.storelogistic.flota.domain.models;

import co.edu.unimagdalena.storelogistic.flota.domain.values.CapacidadCarga;
import co.edu.unimagdalena.storelogistic.flota.domain.values.EstadoVehiculo;
import co.edu.unimagdalena.storelogistic.flota.domain.values.TipoCategoria;
import co.edu.unimagdalena.storelogistic.flota.domain.exceptions.TransicionEstadoInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class VehiculoTest {
    private Categoria categoria;
    private CapacidadCarga capacidadCarga;
    private String idTransportista;

    @BeforeEach
    void setUp() {
        categoria = new Categoria(1L, TipoCategoria.CAMIONETA_URBANA, 
            new CapacidadCarga(BigDecimal.valueOf(1500)));
        capacidadCarga = new CapacidadCarga(BigDecimal.valueOf(1500));
        idTransportista = "TRANS-001";
    }

    @Test
    void testRegistrarNuevoVehiculo() {
        var vehiculo = Vehiculo.registrarNuevo(categoria, capacidadCarga, idTransportista);

        assertNotNull(vehiculo);
        assertEquals(EstadoVehiculo.EN_MANTENIMIENTO, vehiculo.getEstado());
        assertEquals(BigDecimal.ZERO, vehiculo.getPesoActual());
        assertNotNull(vehiculo.getCreatedAt());
    }

    @Test
    void testPorcentajeOcupacion() {
        var vehiculo = Vehiculo.builder()
                .capacidadCarga(new CapacidadCarga(BigDecimal.valueOf(1000)))
                .pesoActual(BigDecimal.valueOf(500))
                .build();

        var porcentaje = vehiculo.porcentajeOcupacion();
        assertEquals(new BigDecimal("50.00"), porcentaje);
    }

    @Test
    void testPorcentajeOcupacion0Porciento() {
        var vehiculo = Vehiculo.builder()
                .capacidadCarga(new CapacidadCarga(BigDecimal.valueOf(1000)))
                .pesoActual(BigDecimal.ZERO)
                .build();

        var porcentaje = vehiculo.porcentajeOcupacion();
        assertEquals(new BigDecimal("0.00"), porcentaje);
    }

    @Test
    void testPorcentajeOcupacion100Porciento() {
        var vehiculo = Vehiculo.builder()
                .capacidadCarga(new CapacidadCarga(BigDecimal.valueOf(1000)))
                .pesoActual(BigDecimal.valueOf(1000))
                .build();

        var porcentaje = vehiculo.porcentajeOcupacion();
        assertEquals(new BigDecimal("100.00"), porcentaje);
    }

    @Test
    void testCambiarEstadoValido() {
        var vehiculo = Vehiculo.registrarNuevo(categoria, capacidadCarga, idTransportista);
        
        vehiculo.cambiarEstado(EstadoVehiculo.DISPONIBLE);
        
        assertEquals(EstadoVehiculo.DISPONIBLE, vehiculo.getEstado());
        assertNotNull(vehiculo.getUpdatedAt());
    }

    @Test
    void testCambiarEstadoInvalido() {
        var vehiculo = Vehiculo.registrarNuevo(categoria, capacidadCarga, idTransportista);
        
        assertThrows(TransicionEstadoInvalidaException.class, 
            () -> vehiculo.cambiarEstado(EstadoVehiculo.EN_RUTA));
    }
}

