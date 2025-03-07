package com.BossLiftingClub.BossLifting.Stripe.RequestsAndResponses;

public class PaymentMethodRequest {
    private String customerId;
    private String paymentMethodId;

    public PaymentMethodRequest() {}
    public PaymentMethodRequest(String customerId, String paymentMethodId) {
        this.customerId = customerId;
        this.paymentMethodId = paymentMethodId;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(String paymentMethodId) { this.paymentMethodId = paymentMethodId; }
}