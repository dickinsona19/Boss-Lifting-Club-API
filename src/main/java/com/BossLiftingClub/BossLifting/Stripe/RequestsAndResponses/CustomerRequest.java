package com.BossLiftingClub.BossLifting.Stripe.RequestsAndResponses;

public class CustomerRequest {
    private String email;
    private String fullName;
    private String paymentMethodId;

    // Default constructor
    public CustomerRequest() {}

    // Parameterized constructor
    public CustomerRequest(String email, String fullName, String paymentMethodId) {
        this.email = email;
        this.fullName = fullName;
        this.paymentMethodId = paymentMethodId;
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(String paymentMethodId) { this.paymentMethodId = paymentMethodId; }
}