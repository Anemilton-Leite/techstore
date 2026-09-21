package com.techstore.api.payment;

import java.math.BigDecimal;

public record GatewayPaymentStatus(
        String externalReference,
        BigDecimal amount,
        String status
) {}