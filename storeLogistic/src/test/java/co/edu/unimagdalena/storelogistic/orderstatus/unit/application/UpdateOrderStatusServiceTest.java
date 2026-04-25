package co.edu.unimagdalena.storelogistic.orderstatus.unit.application;

import co.edu.unimagdalena.storelogistic.orderstatus.application.services.UpdateOrderStatusService;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.exceptions.CarrierNotFoundException;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.exceptions.OrderNotFoundException;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Alert;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Carrier;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Order;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.OrderStatusAudit;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out.AlertRepository;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out.CarrierRepository;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out.OrderRepository;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out.OrderStatusAuditRepository;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out.OrderStatusEventPublisher;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;
import co.edu.unimagdalena.storelogistic.orderstatus.testdata.CarrierFixture;
import co.edu.unimagdalena.storelogistic.orderstatus.testdata.OrderFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateOrderStatusServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock CarrierRepository carrierRepository;
    @Mock AlertRepository alertRepository;
    @Mock OrderStatusAuditRepository auditRepository;
    @Mock OrderStatusEventPublisher eventPublisher;

    @InjectMocks UpdateOrderStatusService service;

    @Test
    @DisplayName("SC1: happy path — persists status, publishes event, no alert for ENTREGADO_COMPLETO")
    void update_entregadoCompleto_savesAndPublishesNoAlert() {
        Order order = OrderFixture.withoutStatus();
        Carrier carrier = CarrierFixture.standard();
        when(carrierRepository.findById(50L)).thenReturn(Optional.of(carrier));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.update(1L, 50L, FinalStatus.ENTREGADO_COMPLETO);

        assertThat(result.finalStatus()).isEqualTo(FinalStatus.ENTREGADO_COMPLETO);
        assertThat(result.effectivenessRate().value()).isEqualTo(100);
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publish(1L, FinalStatus.ENTREGADO_COMPLETO, 50L);
        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("SC1: NO_ENTREGADO — alert is created and saved")
    void update_noEntregado_alertIsSaved() {
        Order order = OrderFixture.withoutStatus();
        when(carrierRepository.findById(50L)).thenReturn(Optional.of(CarrierFixture.standard()));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(1L, 50L, FinalStatus.NO_ENTREGADO);

        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    @DisplayName("SC2: order already has status — audit record is saved")
    void update_orderAlreadyHasStatus_auditIsSaved() {
        Order order = OrderFixture.withStatus(FinalStatus.NO_ENTREGADO, 50L);
        when(carrierRepository.findById(50L)).thenReturn(Optional.of(CarrierFixture.standard()));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(1L, 50L, FinalStatus.ENTREGADO_COMPLETO);

        ArgumentCaptor<OrderStatusAudit> auditCaptor = ArgumentCaptor.forClass(OrderStatusAudit.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().previousStatus()).isEqualTo(FinalStatus.NO_ENTREGADO);
        assertThat(auditCaptor.getValue().newStatus()).isEqualTo(FinalStatus.ENTREGADO_COMPLETO);
    }

    @Test
    @DisplayName("SC1: first-time status registration — no audit saved")
    void update_firstTimeStatus_noAudit() {
        Order order = OrderFixture.withoutStatus();
        when(carrierRepository.findById(50L)).thenReturn(Optional.of(CarrierFixture.standard()));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(1L, 50L, FinalStatus.ENTREGADO_COMPLETO);

        verify(auditRepository, never()).save(any());
    }

    @Test
    @DisplayName("EC: carrier not found → throws CarrierNotFoundException")
    void update_carrierNotFound_throwsException() {
        when(carrierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, 99L, FinalStatus.ENTREGADO_COMPLETO))
                .isInstanceOf(CarrierNotFoundException.class);

        verifyNoInteractions(orderRepository, eventPublisher, alertRepository);
    }

    @Test
    @DisplayName("EC: order not found → throws OrderNotFoundException")
    void update_orderNotFound_throwsException() {
        when(carrierRepository.findById(50L)).thenReturn(Optional.of(CarrierFixture.standard()));
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, 50L, FinalStatus.ENTREGADO_COMPLETO))
                .isInstanceOf(OrderNotFoundException.class);

        verifyNoInteractions(eventPublisher, alertRepository);
    }

    @Test
    @DisplayName("EC: publisher fails — order is already persisted (no rollback)")
    void update_publisherFails_orderRemainsPersistedNoException() {
        Order order = OrderFixture.withoutStatus();
        when(carrierRepository.findById(50L)).thenReturn(Optional.of(CarrierFixture.standard()));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("RabbitMQ down")).when(eventPublisher).publish(any(), any(), any());

        Order result = service.update(1L, 50L, FinalStatus.ENTREGADO_COMPLETO);

        assertThat(result.finalStatus()).isEqualTo(FinalStatus.ENTREGADO_COMPLETO);
        verify(orderRepository).save(any());
    }
}
