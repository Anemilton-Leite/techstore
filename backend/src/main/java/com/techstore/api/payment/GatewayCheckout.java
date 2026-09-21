package com.techstore.api.payment;

public record GatewayCheckout(
        String externalReference,
        String checkoutUrl,
        String providerPreferenceId
) {}