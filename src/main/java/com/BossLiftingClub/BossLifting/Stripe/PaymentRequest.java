package com.BossLiftingClub.BossLifting.Stripe;

import lombok.Data;

@Data
public class PaymentRequest {
    private long amount; // Amount in cents (e.g., 1000 = $10.00)
    private String currency; // e.g., "usd"
    private String description;
}