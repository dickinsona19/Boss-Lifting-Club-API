package com.BossLiftingClub.BossLifting.Stripe.RequestsAndResponses;

public class CustomerResponse {
    private String customerId;

    public CustomerResponse() {}
    public CustomerResponse(String customerId) { this.customerId = customerId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
}