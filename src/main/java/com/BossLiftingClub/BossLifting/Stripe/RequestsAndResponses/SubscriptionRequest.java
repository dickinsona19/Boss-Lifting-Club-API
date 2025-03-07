package com.BossLiftingClub.BossLifting.Stripe.RequestsAndResponses;

public class SubscriptionRequest {
    private String customerId;
    private String priceId;

    public SubscriptionRequest() {}
    public SubscriptionRequest(String customerId, String priceId) {
        this.customerId = customerId;
        this.priceId = priceId;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getPriceId() { return priceId; }
    public void setPriceId(String priceId) { this.priceId = priceId; }
}