package co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.mapper;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.models.OrderPaymentMethod;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.values.PaymentMethod;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.web.dto.FinancePaymentMethodResponse;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.web.dto.PaymentMethodResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PaymentMethodMapper {

    @Mapping(source = "idPedido", target = "orderId")
    @Mapping(source = "formaPago", target = "paymentMethod", qualifiedByName = "parsePaymentMethod")
    OrderPaymentMethod toDomain(FinancePaymentMethodResponse dto);

    @Mapping(source = "paymentMethod", target = "paymentMethod", qualifiedByName = "serializePaymentMethod")
    PaymentMethodResponse toResponse(OrderPaymentMethod domain);

    @Named("parsePaymentMethod")
    static PaymentMethod parsePaymentMethod(String value) {
        return PaymentMethod.fromString(value);
    }

    @Named("serializePaymentMethod")
    static String serializePaymentMethod(PaymentMethod paymentMethod) {
        return paymentMethod.name();
    }
}
