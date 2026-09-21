package com.techstore.api.dto.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderSummaryResponse(
        Long id,
        Long customerId,
        OffsetDateTime orderDate,
        String status,
        BigDecimal totalAmount
) {}
