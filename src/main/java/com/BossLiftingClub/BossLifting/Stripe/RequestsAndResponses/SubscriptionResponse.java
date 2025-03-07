package com.BossLiftingClub.BossLifting.Stripe.RequestsAndResponses;

public class SubscriptionResponse {
    private String subscriptionId;
    private String status; // e.g., "active", "past_due", "incomplete"

    public SubscriptionResponse(String subscriptionId, String status) {
        this.subscriptionId = subscriptionId;
        this.status = status;
    }

    public String getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(String subscriptionId) { this.subscriptionId = subscriptionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}