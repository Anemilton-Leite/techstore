package com.techstore.api.payment;

import com.techstore.api.entity.Order;
import com.techstore.api.entity.OrderItem;
import com.techstore.api.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
        PreferenceRequest body = new PreferenceRequest(
                order.getItems().stream()
                        .map(item -> new PreferenceItem(
                                item.getProduct().getName(),
                                item.getQuantity(),
                                item.getUnitPrice().setScale(2),
                                "BRL"))
                        .toList(),
                new Payer(order.getCustomer().getEmail()),
                externalReference,
                notificationUrl);

        try {
            PreferenceResponse response = client.post()
                    .uri("/checkout/preferences")
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
        } catch (RestClientResponseException exception) {
            throw new BusinessRuleException(
                    "Mercado Pago rejeitou a preferência (HTTP " + exception.getStatusCode().value()
                            + "): " + exception.getResponseBodyAsString());
        }
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

    private record PreferenceRequest(
            List<PreferenceItem> items,
            Payer payer,
            @com.fasterxml.jackson.annotation.JsonProperty("external_reference") String externalReference,
            @com.fasterxml.jackson.annotation.JsonProperty("notification_url") String notificationUrl) {}

    private record PreferenceItem(
            String title,
            Integer quantity,
            @com.fasterxml.jackson.annotation.JsonProperty("unit_price") BigDecimal unitPrice,
            @com.fasterxml.jackson.annotation.JsonProperty("currency_id") String currencyId) {}

        private record Payer(String email) {}

    private record PreferenceResponse(
            String id,
            @com.fasterxml.jackson.annotation.JsonProperty("init_point") String initPoint,
            @com.fasterxml.jackson.annotation.JsonProperty("sandbox_init_point") String sandboxInitPoint) {
        String checkoutUrl() {
            return sandboxInitPoint != null ? sandboxInitPoint : initPoint;
        }
    }
}