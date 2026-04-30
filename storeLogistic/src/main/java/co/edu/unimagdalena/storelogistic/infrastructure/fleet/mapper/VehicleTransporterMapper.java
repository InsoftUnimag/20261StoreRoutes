package co.edu.unimagdalena.storelogistic.infrastructure.fleet.mapper;

import co.edu.unimagdalena.storelogistic.domain.fleet.models.Vehicle;
import co.edu.unimagdalena.storelogistic.infrastructure.fleet.web.dto.RequestTransporterResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VehicleTransporterMapper {

    @Mapping(target = "idVehiculo", source = "vehicleId")
    @Mapping(target = "idTransportista", source = "transporterId")
    RequestTransporterResponse toResponse(Vehicle vehicle);
}