package com.techstore.api.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateOrderRequest(
        @NotNull Long addressId,
        @NotEmpty @Size(max = 50) List<@Valid OrderItemRequest> items
) {}
