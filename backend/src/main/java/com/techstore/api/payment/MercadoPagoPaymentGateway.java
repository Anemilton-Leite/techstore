package com.techstore.api.payment;

import com.techstore.api.entity.Order;
import com.techstore.api.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MercadoPagoPaymentGateway implements PaymentGateway {
    private final RestClient client;
    private final String accessToken;
    private final String frontendBaseUrl;
    private final String notificationUrl;

    public MercadoPagoPaymentGateway(
            RestClient mercadoPagoRestClient,
            @Value("${mercadopago.access-token:}") String accessToken,
            @Value("${app.frontend-base-url:http://127.0.0.1:5500}") String frontendBaseUrl,
            @Value("${mercadopago.notification-url:}") String notificationUrl) {
        this.client = mercadoPagoRestClient;
        this.accessToken = accessToken;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/$", "");
        this.notificationUrl = notificationUrl;
    }

    @Override
    public GatewayCheckout createCheckout(Order order) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessRuleException("Mercado Pago não está configurado.");
        }
        if (notificationUrl == null || notificationUrl.isBlank()) {
            throw new BusinessRuleException("URL pública de webhook do Mercado Pago não está configurada.");
        }

        String externalReference = "TECHSTORE-ORDER-" + order.getId();
        OrderRequest body = new OrderRequest(
            "online",
            order.getTotalAmount().setScale(2),
                externalReference,
            "manual",
            new Payer(order.getCustomer().getEmail()),
            new OrderConfig(
                notificationUrl,
                new OnlineConfig(
                    confirmationUrl(order),
                    confirmationUrl(order),
                    confirmationUrl(order),
                    "approved"))
        );

        PreferenceResponse response = client.post()
            .uri("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
            .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .body(body)
                .retrieve()
                .body(PreferenceResponse.class);

        if (response == null || response.id() == null || response.checkoutUrl() == null) {
            throw new BusinessRuleException("Mercado Pago não retornou uma URL de checkout.");
        }
        return new GatewayCheckout(externalReference, response.checkoutUrl(), response.id());
    }

    @Override
    public GatewayPaymentStatus getPayment(String paymentId) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessRuleException("Mercado Pago não está configurado.");
        }

        Map<?, ?> response = client.get()
                .uri("/v1/payments/{id}", paymentId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);
        if (response == null) {
            throw new BusinessRuleException("Mercado Pago não retornou o pagamento.");
        }

        Object transactionAmount = response.get("transaction_amount");
        return new GatewayPaymentStatus(
                stringValue(response.get("external_reference")),
                transactionAmount instanceof Number number
                        ? BigDecimal.valueOf(number.doubleValue())
                        : new BigDecimal(String.valueOf(transactionAmount)),
                stringValue(response.get("status")));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String confirmationUrl(Order order) {
        return frontendBaseUrl + "/pages/confirmacao.html?id=" + order.getId();
    }

        private record OrderRequest(
            String type,
            @com.fasterxml.jackson.annotation.JsonProperty("total_amount") BigDecimal totalAmount,
            @com.fasterxml.jackson.annotation.JsonProperty("external_reference") String externalReference,
            @com.fasterxml.jackson.annotation.JsonProperty("processing_mode") String processingMode,
            Payer payer,
            OrderConfig config) {}

        private record Payer(String email) {}

        private record OrderConfig(
            @com.fasterxml.jackson.annotation.JsonProperty("notification_url") String notificationUrl,
            OnlineConfig online) {}

        private record OnlineConfig(
            @com.fasterxml.jackson.annotation.JsonProperty("success_url") String successUrl,
            @com.fasterxml.jackson.annotation.JsonProperty("failure_url") String failureUrl,
            @com.fasterxml.jackson.annotation.JsonProperty("pending_url") String pendingUrl,
            @com.fasterxml.jackson.annotation.JsonProperty("auto_return") String autoReturn) {}

    private record PreferenceResponse(
            String id,
            @com.fasterxml.jackson.annotation.JsonProperty("checkout_url") String checkoutUrl) {}
}