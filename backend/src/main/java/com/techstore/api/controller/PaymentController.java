package com.techstore.api.controller;

import com.techstore.api.dto.payment.PaymentRequest;
import com.techstore.api.dto.payment.PaymentResponse;
import com.techstore.api.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Pagamentos")
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Criar pagamento para pedido")
    public ResponseEntity<PaymentResponse> createPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(orderId, request));
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Consultar pagamento")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPayment(orderId));
    }

    /* SOMENTE TESTE. Não utilizar como webhook de produção. */
    @PostMapping("/webhooks/test/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Simular webhook de pagamento")
    public ResponseEntity<PaymentResponse> processTestWebhook(
            @PathVariable Long orderId,
            @RequestParam String event,
            @RequestParam(required = false) String providerReference) {
        return ResponseEntity.ok(paymentService.processTestWebhook(orderId, event, providerReference));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Recebe webhook assinado do gateway")
    public ResponseEntity<Void> processGatewayWebhook(
            @RequestParam(name = "data.id") String paymentId,
            @RequestHeader(name = "x-signature", required = false) String signature,
            @RequestHeader(name = "x-request-id", required = false) String requestId) {
        logger.info("Mercado Pago webhook recebido: dataIdPresent={}, signaturePresent={}, requestIdPresent={}, signatureLength={}",
                paymentId != null && !paymentId.isBlank(),
                signature != null && !signature.isBlank(),
                requestId != null && !requestId.isBlank(),
                signature == null ? 0 : signature.length());
        paymentService.processGatewayWebhook(paymentId, signature, requestId);
        return ResponseEntity.ok().build();
    }
}
