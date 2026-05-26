package co.edu.unimagdalena.storelogistic.infrastructure.paymentmethod.mapper;

import co.edu.unimagdalena.storelogistic.domain.paymentmethod.models.OrderPaymentMethod;
import co.edu.unimagdalena.storelogistic.domain.paymentmethod.values.PaymentMethod;
import co.edu.unimagdalena.storelogistic.infrastructure.paymentmethod.web.dto.FinancePaymentMethodResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PaymentMethodMapper {

    @Mapping(source = "idPedido", target = "orderId")
    @Mapping(source = "formaPago", target = "paymentMethod", qualifiedByName = "parsePaymentMethod")
    @Mapping(source = "valorContraEntrega", target = "totalPedido")
    OrderPaymentMethod toDomain(FinancePaymentMethodResponse dto);

    @Named("parsePaymentMethod")
    static PaymentMethod parsePaymentMethod(String value) {
        return PaymentMethod.fromString(value);
    }
}
