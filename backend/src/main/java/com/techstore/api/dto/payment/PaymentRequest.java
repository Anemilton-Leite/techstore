package com.techstore.api.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PaymentRequest(
        @NotBlank(message = "paymentMethod é obrigatório")
        @Pattern(regexp = "PIX|CREDIT_CARD", message = "Método de pagamento inválido")
        String paymentMethod
) {
}
