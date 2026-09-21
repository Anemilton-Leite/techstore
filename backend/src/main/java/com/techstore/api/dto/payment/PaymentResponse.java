package com.techstore.api.dto.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        BigDecimal amount,
        String status,
        String paymentMethod,
        String providerReference,
        String checkoutUrl,
        OffsetDateTime createdAt
) {
}
