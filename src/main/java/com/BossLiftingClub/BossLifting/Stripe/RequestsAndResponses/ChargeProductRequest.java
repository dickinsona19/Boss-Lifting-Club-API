package com.BossLiftingClub.BossLifting.Stripe.RequestsAndResponses;

public class ChargeProductRequest {
    private String customerId;
    private String productId;

    public ChargeProductRequest() {}
    public ChargeProductRequest(String customerId, String productId) {
        this.customerId = customerId;
        this.productId = productId;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
}