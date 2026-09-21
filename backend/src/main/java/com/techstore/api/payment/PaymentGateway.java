package com.techstore.api.payment;

import com.techstore.api.entity.Order;

public interface PaymentGateway {
    GatewayCheckout createCheckout(Order order);
    GatewayPaymentStatus getPayment(String paymentId);
}