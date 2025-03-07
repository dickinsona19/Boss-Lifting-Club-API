package com.BossLiftingClub.BossLifting.Stripe;

import lombok.Data;

@Data
public class StripeProductResponse {
    private String productId;
    private String priceId;
}