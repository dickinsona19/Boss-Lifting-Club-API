package com.BossLiftingClub.BossLifting.Stripe.RequestsAndResponses;

public class FoundingMemberSubscriptionRequest {
    private String customerId;
    private String priceId;
    private String couponId;

    public FoundingMemberSubscriptionRequest() {}
    public FoundingMemberSubscriptionRequest(String customerId, String priceId, String couponId) {
        this.customerId = customerId;
        this.priceId = priceId;
        this.couponId = couponId;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getPriceId() { return priceId; }
    public void setPriceId(String priceId) { this.priceId = priceId; }
    public String getCouponId() { return couponId; }
    public void setCouponId(String couponId) { this.couponId = couponId; }
}