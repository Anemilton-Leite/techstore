package com.techstore.api.service.impl;

import com.techstore.api.entity.Order;
import com.techstore.api.entity.OrderItem;
import com.techstore.api.entity.OrderStatus;
import com.techstore.api.entity.Product;
import com.techstore.api.entity.User;
import com.techstore.api.exception.BusinessRuleException;
import com.techstore.api.repository.AddressRepository;
import com.techstore.api.repository.OrderRepository;
import com.techstore.api.repository.ProductRepository;
import com.techstore.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre o ciclo de vida crítico do pedido usado pelo painel admin:
 * PENDENTE -> PAGO, PENDENTE -> CANCELADO (com devolução de estoque) e a
 * imutabilidade dos estados terminais (PAGO, CANCELADO).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private OrderIdempotencyRecovery idempotencyRecovery;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, productRepository, userRepository, addressRepository, idempotencyRecovery);
    }

    private Order pendingOrderWithItem(Product product, int quantity) {
        User customer = org.mockito.Mockito.mock(User.class);
        Order order = new Order(customer, null, OrderStatus.PENDENTE, BigDecimal.valueOf(100));
        order.addItem(new OrderItem(product, quantity, product.getPrice()));
        return order;
    }

    @Test
    void updateStatusMarksPendingOrderAsPaid() {
        Order order = pendingOrderWithItem(new Product("SSD", "desc", BigDecimal.valueOf(300), 4, null, null), 1);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        var response = orderService.updateStatus(1L, OrderStatus.PAGO);

        assertEquals("PAGO", response.status());
        verify(productRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void updateStatusCancelsPendingOrderAndRestoresStock() {
        Product product = new Product("Placa de vídeo", "desc", BigDecimal.valueOf(2000), 2, null, null);
        Order order = pendingOrderWithItem(product, 3);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(productRepository.findByIdForUpdate(any())).thenReturn(Optional.of(product));

        var response = orderService.updateStatus(2L, OrderStatus.CANCELADO);

        assertEquals("CANCELADO", response.status());
        assertEquals(5, product.getStock(), "estoque deve ser devolvido (2 + 3)");
        verify(productRepository).save(product);
    }

    @Test
    void updateStatusRejectsChangesOnceOrderIsPaid() {
        Order order = pendingOrderWithItem(new Product("Fonte", "desc", BigDecimal.valueOf(400), 1, null, null), 1);
        order.setStatus(OrderStatus.PAGO);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThrows(BusinessRuleException.class, () -> orderService.updateStatus(3L, OrderStatus.CANCELADO));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateStatusRejectsReopeningCancelledOrder() {
        Order order = pendingOrderWithItem(new Product("Gabinete", "desc", BigDecimal.valueOf(250), 1, null, null), 1);
        order.setStatus(OrderStatus.CANCELADO);
        when(orderRepository.findById(4L)).thenReturn(Optional.of(order));

        assertThrows(BusinessRuleException.class, () -> orderService.updateStatus(4L, OrderStatus.PENDENTE));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateStatusIsNoOpWhenStatusIsUnchanged() {
        Order order = pendingOrderWithItem(new Product("Mousepad", "desc", BigDecimal.valueOf(30), 5, null, null), 1);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        var response = orderService.updateStatus(5L, OrderStatus.PENDENTE);

        assertEquals("PENDENTE", response.status());
        verify(orderRepository, never()).save(any());
    }
}
