package co.edu.unimagdalena.storelogistic.flota.application.services;

import co.edu.unimagdalena.storelogistic.flota.domain.models.Vehiculo;
import co.edu.unimagdalena.storelogistic.flota.domain.models.Categoria;
import co.edu.unimagdalena.storelogistic.flota.domain.ports.in.RegistrarVehiculoUseCase;
import co.edu.unimagdalena.storelogistic.flota.domain.ports.out.VehiculoRepository;
import co.edu.unimagdalena.storelogistic.flota.domain.ports.out.CategoriaRepository;
import co.edu.unimagdalena.storelogistic.flota.domain.values.CapacidadCarga;
import co.edu.unimagdalena.storelogistic.flota.domain.values.TipoCategoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrarVehiculoService implements RegistrarVehiculoUseCase {
    private final VehiculoRepository vehiculoRepository;
    private final CategoriaRepository categoriaRepository;

    @Override
    public Vehiculo registrar(TipoCategoria categoria, CapacidadCarga capacidadCarga, String idTransportista) {
        log.info("Registrando nuevo vehículo: categoria={}, capacidad={}, transportista={}",
                categoria, capacidadCarga, idTransportista);

        var cat = categoriaRepository.findByTipo(categoria)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + categoria));

        var vehiculo = Vehiculo.registrarNuevo(cat, capacidadCarga, idTransportista);
        var guardado = vehiculoRepository.save(vehiculo);

        log.info("Vehículo registrado exitosamente con ID: {}", guardado.getIdVehiculo());
        return guardado;
    }
}
