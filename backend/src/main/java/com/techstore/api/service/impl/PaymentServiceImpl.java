package com.techstore.api.service.impl;

import com.techstore.api.dto.payment.PaymentRequest;
import com.techstore.api.dto.payment.PaymentResponse;
import com.techstore.api.entity.Order;
import com.techstore.api.entity.OrderStatus;
import com.techstore.api.entity.Payment;
import com.techstore.api.entity.PaymentStatus;
import com.techstore.api.entity.Product;
import com.techstore.api.entity.User;
import com.techstore.api.payment.GatewayCheckout;
import com.techstore.api.exception.BusinessRuleException;
import com.techstore.api.exception.ResourceNotFoundException;
import com.techstore.api.repository.OrderRepository;
import com.techstore.api.repository.PaymentRepository;
import com.techstore.api.repository.ProductRepository;
import com.techstore.api.repository.UserRepository;
import com.techstore.api.payment.GatewayPaymentStatus;
import com.techstore.api.payment.PaymentGateway;
import com.techstore.api.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentGateway paymentGateway;
    private final String webhookSecret;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            PaymentGateway paymentGateway,
            @Value("${mercadopago.webhook-secret:}") String webhookSecret) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.paymentGateway = paymentGateway;
        this.webhookSecret = webhookSecret;
    }

    @Override
    @Transactional
    public PaymentResponse createPayment(Long orderId, PaymentRequest request) {
        User user = getAuthenticatedUser();
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));

        Payment existing = paymentRepository.findByOrderId(orderId).orElse(null);
        if (order.getStatus() == OrderStatus.CANCELADO) {
            throw new BusinessRuleException("Não é possível pagar um pedido cancelado.");
        }
        if (order.getStatus() == OrderStatus.PAGO && existing == null) {
            throw new BusinessRuleException("O pedido já está pago.");
        }

        if (existing != null) {
            if (existing.getStatus() == PaymentStatus.PENDENTE) {
                return toResponse(existing);
            }
            if (existing.getStatus() == PaymentStatus.RECUSADO) {
                GatewayCheckout checkout = paymentGateway.createCheckout(order);
                existing.setStatus(PaymentStatus.PENDENTE);
                existing.setPaymentMethod(request.paymentMethod());
                existing.setProviderReference(checkout.providerPreferenceId());
                existing.setExternalReference(checkout.externalReference());
                existing.setCheckoutUrl(checkout.checkoutUrl());
                existing.setProviderPreferenceId(checkout.providerPreferenceId());
                return toResponse(paymentRepository.save(existing));
            }
            throw new BusinessRuleException("Este pedido já possui um pagamento finalizado.");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDENTE);
        payment.setPaymentMethod(request.paymentMethod());
        Payment saved = paymentRepository.save(payment);
        GatewayCheckout checkout = paymentGateway.createCheckout(order);
        saved.setProviderReference(checkout.providerPreferenceId());
        saved.setExternalReference(checkout.externalReference());
        saved.setCheckoutUrl(checkout.checkoutUrl());
        saved.setProviderPreferenceId(checkout.providerPreferenceId());

        return toResponse(paymentRepository.save(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long orderId) {
        User user = getAuthenticatedUser();
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));

        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado."));
        return toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse processTestWebhook(Long orderId, String event, String providerReference) {
        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado."));

        String normalizedEvent = event.trim().toUpperCase();
        if (providerReference != null && !providerReference.isBlank()
                && !providerReference.equals(payment.getProviderReference())) {
            throw new BusinessRuleException("Referência do pagamento inválida.");
        }
        if (payment.getStatus() == PaymentStatus.APROVADO) {
            return toResponse(payment);
        }
        if (payment.getStatus() == PaymentStatus.CANCELADO) {
            throw new BusinessRuleException("Pagamento cancelado não pode ser aprovado.");
        }

        switch (normalizedEvent) {
            case "PAYMENT_APPROVED" -> {
                payment.setStatus(PaymentStatus.APROVADO);
                payment.getOrder().setStatus(OrderStatus.PAGO);
            }
            case "PAYMENT_DECLINED" -> payment.setStatus(PaymentStatus.RECUSADO);
            case "PAYMENT_CANCELLED" -> {
                restoreOrderStock(payment.getOrder());
                payment.setStatus(PaymentStatus.CANCELADO);
                payment.getOrder().setStatus(OrderStatus.CANCELADO);
            }
            default -> throw new BusinessRuleException("Evento de pagamento inválido.");
        }

        Payment saved = paymentRepository.save(payment);
        orderRepository.save(payment.getOrder());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void processGatewayWebhook(String paymentId, String signature, String requestId) {
        validateWebhookSignature(paymentId, signature, requestId);
        GatewayPaymentStatus gatewayPayment = paymentGateway.getPayment(paymentId);
        if (gatewayPayment.externalReference() == null || !gatewayPayment.externalReference().startsWith("TECHSTORE-ORDER-")) {
            throw new BusinessRuleException("Referência externa do pagamento inválida.");
        }

        Long orderId;
        try {
            orderId = Long.valueOf(gatewayPayment.externalReference().substring("TECHSTORE-ORDER-".length()));
        } catch (NumberFormatException exception) {
            throw new BusinessRuleException("Referência externa do pagamento inválida.");
        }

        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado."));
        if (!gatewayPayment.externalReference().equals(payment.getExternalReference())) {
            throw new BusinessRuleException("Referência do pagamento inválida.");
        }
        if (gatewayPayment.amount() == null
            || !payment.getAmount().setScale(2, RoundingMode.HALF_UP)
                .equals(gatewayPayment.amount().setScale(2, RoundingMode.HALF_UP))) {
            throw new BusinessRuleException("Valor do pagamento inválido.");
        }
        if (payment.getStatus() == PaymentStatus.APROVADO) return;
        if (payment.getStatus() == PaymentStatus.CANCELADO) {
            if ("cancelled".equals(gatewayPayment.status())) return;
            throw new BusinessRuleException("Pagamento cancelado não pode ser aprovado.");
        }

        switch (gatewayPayment.status()) {
            case "approved" -> {
                payment.setStatus(PaymentStatus.APROVADO);
                payment.getOrder().setStatus(OrderStatus.PAGO);
            }
            case "rejected" -> payment.setStatus(PaymentStatus.RECUSADO);
            case "cancelled" -> {
                restoreOrderStock(payment.getOrder());
                payment.setStatus(PaymentStatus.CANCELADO);
                payment.getOrder().setStatus(OrderStatus.CANCELADO);
            }
            case "pending", "in_process" -> payment.setStatus(PaymentStatus.PENDENTE);
            default -> throw new BusinessRuleException("Status de pagamento do gateway inválido.");
        }
        paymentRepository.save(payment);
        orderRepository.save(payment.getOrder());
    }

    private void validateWebhookSignature(String paymentId, String signature, String requestId) {
        if (webhookSecret == null || webhookSecret.isBlank() || signature == null || requestId == null) {
            throw new BusinessRuleException("Assinatura do webhook inválida.");
        }
        String timestamp = null;
        String providedHash = null;
        for (String part : signature.split(",")) {
            String[] values = part.trim().split("=", 2);
            if (values.length != 2) continue;
            if (values[0].equals("ts")) timestamp = values[1];
            if (values[0].equals("v1")) providedHash = values[1];
        }
        if (timestamp == null || providedHash == null) {
            throw new BusinessRuleException("Assinatura do webhook inválida.");
        }
        try {
            long timestampSeconds = Long.parseLong(timestamp);
            if (Math.abs(Duration.between(Instant.ofEpochSecond(timestampSeconds), Instant.now()).toSeconds()) > 300) {
                throw new BusinessRuleException("Assinatura do webhook expirada.");
            }
        } catch (NumberFormatException exception) {
            throw new BusinessRuleException("Assinatura do webhook inválida.");
        }
        String manifest = "id:" + paymentId + ";request-id:" + requestId + ";ts:" + timestamp + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expectedHash = hex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
            if (!MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.UTF_8), providedHash.getBytes(StandardCharsets.UTF_8))) {
                throw new BusinessRuleException("Assinatura do webhook inválida.");
            }
        } catch (Exception exception) {
            if (exception instanceof BusinessRuleException businessRuleException) throw businessRuleException;
            throw new BusinessRuleException("Não foi possível validar a assinatura do webhook.");
        }
    }

    private String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) value.append(String.format("%02x", item));
        return value.toString();
    }

    private void restoreOrderStock(Order order) {
        order.getItems().forEach(item -> {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produto não encontrado: " + item.getProduct().getId()));
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        });
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado."));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getPaymentMethod(),
                payment.getProviderReference(),
                payment.getCheckoutUrl(),
                payment.getCreatedAt());
    }
}
