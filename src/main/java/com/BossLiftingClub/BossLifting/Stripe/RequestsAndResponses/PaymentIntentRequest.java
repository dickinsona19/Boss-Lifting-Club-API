package com.BossLiftingClub.BossLifting.Stripe.RequestsAndResponses;

public class PaymentIntentRequest {
    private String customerId;
    private String priceId; // New field for Stripe Price ID (e.g., "price_xxxx")
    // Remove "amount" since we'll fetch it from the price

    // Default constructor
    public PaymentIntentRequest() {}

    // Parameterized constructor
    public PaymentIntentRequest(String customerId, String priceId) {
        this.customerId = customerId;
        this.priceId = priceId;
    }

    // Getters and Setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getPriceId() { return priceId; }
    public void setPriceId(String priceId) { this.priceId = priceId; }
}