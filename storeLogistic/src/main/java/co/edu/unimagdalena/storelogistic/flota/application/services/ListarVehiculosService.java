package co.edu.unimagdalena.storelogistic.flota.application.services;

import co.edu.unimagdalena.storelogistic.flota.domain.models.Vehiculo;
import co.edu.unimagdalena.storelogistic.flota.domain.ports.in.ListarVehiculosUseCase;
import co.edu.unimagdalena.storelogistic.flota.domain.ports.out.VehiculoRepository;
import co.edu.unimagdalena.storelogistic.flota.domain.values.FiltroVehiculo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListarVehiculosService implements ListarVehiculosUseCase {
    private final VehiculoRepository vehiculoRepository;

    @Override
    public List<Vehiculo> listar(FiltroVehiculo filtro) {
        log.info("Listando vehículos con filtros: {}", filtro);
        return vehiculoRepository.findWithFilters(filtro);
    }
}
