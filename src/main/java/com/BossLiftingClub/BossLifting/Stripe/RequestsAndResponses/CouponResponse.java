package com.BossLiftingClub.BossLifting.Stripe.RequestsAndResponses;

public class CouponResponse {
    private String couponId;

    public CouponResponse() {}
    public CouponResponse(String couponId) { this.couponId = couponId; }

    public String getCouponId() { return couponId; }
    public void setCouponId(String couponId) { this.couponId = couponId; }
}