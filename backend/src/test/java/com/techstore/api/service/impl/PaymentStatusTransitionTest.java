package com.techstore.api.service.impl;

import com.techstore.api.entity.Order;
import com.techstore.api.entity.OrderItem;
import com.techstore.api.entity.OrderStatus;
import com.techstore.api.entity.Payment;
import com.techstore.api.entity.PaymentStatus;
import com.techstore.api.entity.Product;
import com.techstore.api.entity.User;
import com.techstore.api.exception.BusinessRuleException;
import com.techstore.api.payment.GatewayPaymentStatus;
import com.techstore.api.payment.PaymentGateway;
import com.techstore.api.repository.OrderRepository;
import com.techstore.api.repository.PaymentRepository;
import com.techstore.api.repository.ProductRepository;
import com.techstore.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Cobre o ciclo de vida crítico do pagamento: PENDENTE -> APROVADO/RECUSADO/CANCELADO,
 * o comportamento idempotente em entregas de webhook repetidas/concorrentes e a
 * devolução de estoque no cancelamento. Exercita tanto o endpoint administrativo de
 * teste quanto o webhook público e assinado do gateway (fluxo real de produção).
 */
@ExtendWith(MockitoExtension.class)
class PaymentStatusTransitionTest {
    private static final String WEBHOOK_SECRET = "unit-test-webhook-secret";

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

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository, orderRepository, productRepository, userRepository,
                paymentGateway, WEBHOOK_SECRET);
    }

    private Order orderWithStock(Product product, int quantity, BigDecimal totalAmount) {
        User customer = org.mockito.Mockito.mock(User.class);
        Order order = new Order(customer, null, OrderStatus.PENDENTE, totalAmount);
        order.addItem(new OrderItem(product, quantity, product.getPrice()));
        return order;
    }

    private Payment pendingPayment(Order order, String externalReference) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDENTE);
        payment.setPaymentMethod("PIX");
        payment.setExternalReference(externalReference);
        return payment;
    }

    private String sign(String paymentId, String requestId, String timestamp) throws Exception {
        String manifest = "id:" + paymentId + ";request-id:" + requestId + ";ts:" + timestamp + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte value : hash) hex.append(String.format("%02x", value));
        return "ts=" + timestamp + ",v1=" + hex;
    }

    // ---------- endpoint administrativo de teste ----------

    @Test
    void testWebhookApprovesPaymentAndMarksOrderPaid() {
        Product product = new Product("Mouse", "desc", BigDecimal.valueOf(50), 3, null, null);
        Order order = orderWithStock(product, 2, BigDecimal.valueOf(100));
        Payment payment = pendingPayment(order, "TECHSTORE-ORDER-1");

        when(paymentRepository.findByOrderIdForUpdate(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        var response = paymentService.processTestWebhook(1L, "PAYMENT_APPROVED", null);

        assertEquals(PaymentStatus.APROVADO.name(), response.status());
        assertEquals(OrderStatus.PAGO, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void testWebhookDeclinesPaymentWithoutTouchingOrderStatus() {
        Product product = new Product("Teclado", "desc", BigDecimal.valueOf(80), 1, null, null);
        Order order = orderWithStock(product, 1, BigDecimal.valueOf(80));
        Payment payment = pendingPayment(order, "TECHSTORE-ORDER-2");

        when(paymentRepository.findByOrderIdForUpdate(2L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        var response = paymentService.processTestWebhook(2L, "PAYMENT_DECLINED", null);

        assertEquals(PaymentStatus.RECUSADO.name(), response.status());
        assertEquals(OrderStatus.PENDENTE, order.getStatus());
        verify(productRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void testWebhookCancelsPaymentAndRestoresStock() {
        Product product = new Product("Monitor", "desc", BigDecimal.valueOf(500), 3, null, null);
        Order order = orderWithStock(product, 2, BigDecimal.valueOf(1000));
        Payment payment = pendingPayment(order, "TECHSTORE-ORDER-3");

        when(paymentRepository.findByOrderIdForUpdate(3L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(productRepository.findByIdForUpdate(any())).thenReturn(Optional.of(product));

        var response = paymentService.processTestWebhook(3L, "PAYMENT_CANCELLED", null);

        assertEquals(PaymentStatus.CANCELADO.name(), response.status());
        assertEquals(OrderStatus.CANCELADO, order.getStatus());
        assertEquals(5, product.getStock(), "estoque deve ser devolvido (3 + 2)");
        verify(productRepository).save(product);
    }

    @Test
    void testWebhookIsIdempotentForAlreadyApprovedPayment() {
        Product product = new Product("SSD", "desc", BigDecimal.valueOf(300), 4, null, null);
        Order order = orderWithStock(product, 1, BigDecimal.valueOf(300));
        order.setStatus(OrderStatus.PAGO);
        Payment payment = pendingPayment(order, "TECHSTORE-ORDER-4");
        payment.setStatus(PaymentStatus.APROVADO);

        when(paymentRepository.findByOrderIdForUpdate(4L)).thenReturn(Optional.of(payment));

        var response = paymentService.processTestWebhook(4L, "PAYMENT_APPROVED", null);

        assertEquals(PaymentStatus.APROVADO.name(), response.status());
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testWebhookRejectsReapprovalOfCancelledPayment() {
        Product product = new Product("Webcam", "desc", BigDecimal.valueOf(150), 2, null, null);
        Order order = orderWithStock(product, 1, BigDecimal.valueOf(150));
        Payment payment = pendingPayment(order, "TECHSTORE-ORDER-5");
        payment.setStatus(PaymentStatus.CANCELADO);

        when(paymentRepository.findByOrderIdForUpdate(5L)).thenReturn(Optional.of(payment));

        assertThrows(BusinessRuleException.class,
                () -> paymentService.processTestWebhook(5L, "PAYMENT_APPROVED", null));
    }

    // ---------- webhook público e assinado do gateway (fluxo real) ----------

    @Test
    void gatewayWebhookApprovesPaymentWhenSignatureIsValid() throws Exception {
        Product product = new Product("Headset", "desc", BigDecimal.valueOf(100), 5, null, null);
        Order order = orderWithStock(product, 1, BigDecimal.valueOf(100));
        Payment payment = pendingPayment(order, "TECHSTORE-ORDER-10");

        when(paymentRepository.findByOrderIdForUpdate(10L)).thenReturn(Optional.of(payment));
        when(paymentGateway.getPayment("999")).thenReturn(
                new GatewayPaymentStatus("TECHSTORE-ORDER-10", BigDecimal.valueOf(100), "approved"));
        when(paymentRepository.save(payment)).thenReturn(payment);

        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = sign("999", "req-1", timestamp);

        paymentService.processGatewayWebhook("999", signature, "req-1");

        assertEquals(PaymentStatus.APROVADO, payment.getStatus());
        assertEquals(OrderStatus.PAGO, order.getStatus());
    }

    @Test
    void gatewayWebhookCancelsPaymentAndRestoresStockWhenSignatureIsValid() throws Exception {
        Product product = new Product("Cadeira Gamer", "desc", BigDecimal.valueOf(700), 2, null, null);
        Order order = orderWithStock(product, 1, BigDecimal.valueOf(700));
        Payment payment = pendingPayment(order, "TECHSTORE-ORDER-11");

        when(paymentRepository.findByOrderIdForUpdate(11L)).thenReturn(Optional.of(payment));
        when(paymentGateway.getPayment("1000")).thenReturn(
                new GatewayPaymentStatus("TECHSTORE-ORDER-11", BigDecimal.valueOf(700), "cancelled"));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(productRepository.findByIdForUpdate(any())).thenReturn(Optional.of(product));

        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = sign("1000", "req-2", timestamp);

        paymentService.processGatewayWebhook("1000", signature, "req-2");

        assertEquals(PaymentStatus.CANCELADO, payment.getStatus());
        assertEquals(OrderStatus.CANCELADO, order.getStatus());
        assertEquals(3, product.getStock(), "estoque deve ser devolvido (2 + 1)");
    }

    @Test
    void gatewayWebhookRejectsTamperedSignature() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        assertThrows(BusinessRuleException.class, () -> paymentService.processGatewayWebhook(
                "999", "ts=" + timestamp + ",v1=0000invalidhash0000", "req-1"));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    void gatewayWebhookRejectsExpiredSignature() throws Exception {
        String staleTimestamp = String.valueOf(Instant.now().minusSeconds(600).getEpochSecond());
        String signature = sign("999", "req-1", staleTimestamp);

        assertThrows(BusinessRuleException.class,
                () -> paymentService.processGatewayWebhook("999", signature, "req-1"));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    void gatewayWebhookRejectsMissingSignatureHeaders() {
        assertThrows(BusinessRuleException.class,
                () -> paymentService.processGatewayWebhook("999", null, null));
        verifyNoInteractions(paymentGateway);
    }
}
