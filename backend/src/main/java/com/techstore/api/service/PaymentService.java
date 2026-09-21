package com.techstore.api.service;

import com.techstore.api.dto.payment.PaymentRequest;
import com.techstore.api.dto.payment.PaymentResponse;

public interface PaymentService {
    PaymentResponse createPayment(Long orderId, PaymentRequest request);
    PaymentResponse getPayment(Long orderId);
    PaymentResponse processTestWebhook(Long orderId, String event, String providerReference);
    void processGatewayWebhook(String paymentId, String signature, String requestId);
}
