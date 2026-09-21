package com.techstore.api.service.impl;

import com.techstore.api.dto.payment.PaymentRequest;
import com.techstore.api.entity.Order;
import com.techstore.api.entity.OrderStatus;
import com.techstore.api.entity.Payment;
import com.techstore.api.entity.PaymentStatus;
import com.techstore.api.entity.User;
import com.techstore.api.payment.GatewayCheckout;
import com.techstore.api.payment.PaymentGateway;
import com.techstore.api.repository.OrderRepository;
import com.techstore.api.repository.PaymentRepository;
import com.techstore.api.repository.ProductRepository;
import com.techstore.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentGateway paymentGateway;
    @InjectMocks
    private PaymentServiceImpl paymentService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void retriesDeclinedPaymentWithNewPendingReference() {
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user@example.com", null));

        Order order = new Order(user, null, OrderStatus.PENDENTE, BigDecimal.TEN);
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(BigDecimal.TEN);
        payment.setStatus(PaymentStatus.RECUSADO);
        payment.setPaymentMethod("PIX");
        payment.setProviderReference("TEST-old");

        when(orderRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(payment));
        when(paymentGateway.createCheckout(order))
                .thenReturn(new GatewayCheckout("TECHSTORE-ORDER-10", "https://sandbox.example/checkout", "pref-10"));
        when(paymentRepository.save(payment)).thenReturn(payment);

        var response = paymentService.createPayment(10L, new PaymentRequest("CREDIT_CARD"));

        assertEquals(PaymentStatus.PENDENTE.name(), response.status());
        assertEquals("CREDIT_CARD", response.paymentMethod());
        assertEquals("pref-10", response.providerReference());
        assertEquals("https://sandbox.example/checkout", response.checkoutUrl());
        verify(paymentRepository).save(payment);
    }

    @Test
    void rejectsRetryForApprovedPaymentAsFinalized() {
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user@example.com", null));

        Order order = new Order(user, null, OrderStatus.PAGO, BigDecimal.TEN);
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setStatus(PaymentStatus.APROVADO);

        when(orderRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(payment));

        var exception = assertThrows(RuntimeException.class,
                () -> paymentService.createPayment(10L, new PaymentRequest("PIX")));

        assertEquals("Este pedido já possui um pagamento finalizado.", exception.getMessage());
    }

    @Test
    void doesNotExposePaymentFromAnotherUser() {
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user@example.com", null));
        when(orderRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentService.getPayment(10L));
        verify(paymentRepository, never()).findByOrderId(10L);
    }

    @Test
    void createsFreshPendingPaymentAndPersistsGatewayCheckoutDetails() {
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user@example.com", null));

        Order order = new Order(user, null, OrderStatus.PENDENTE, BigDecimal.valueOf(150));
        when(orderRepository.findByIdAndUserId(20L, 7L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(20L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGateway.createCheckout(order))
                .thenReturn(new GatewayCheckout("TECHSTORE-ORDER-20", "https://sandbox.example/checkout/20", "pref-20"));

        var response = paymentService.createPayment(20L, new PaymentRequest("PIX"));

        assertEquals(PaymentStatus.PENDENTE.name(), response.status());
        assertEquals("pref-20", response.providerReference());
        assertEquals("https://sandbox.example/checkout/20", response.checkoutUrl());
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void blocksNewPaymentForCancelledOrder() {
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user@example.com", null));

        Order order = new Order(user, null, OrderStatus.CANCELADO, BigDecimal.TEN);
        when(orderRepository.findByIdAndUserId(20L, 7L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(20L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> paymentService.createPayment(20L, new PaymentRequest("PIX")));
        verify(paymentGateway, never()).createCheckout(any());
    }
}