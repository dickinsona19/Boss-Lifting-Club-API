package com.BossLiftingClub.BossLifting.Stripe;

import lombok.Data;

@Data
public class PaymentResponse {
    // You can remove these if they're not needed with Checkout Sessions,
    // or keep them if you want to support both flows.
    private String paymentIntentId;
    private String clientSecret;

    // New field for the payment link from Stripe Checkout
    private String paymentLink;
}
