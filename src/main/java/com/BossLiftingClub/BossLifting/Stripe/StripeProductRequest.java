package com.BossLiftingClub.BossLifting.Stripe;

public class StripeProductRequest {
    private String name;
    private String description;
    private long unitAmount;
    private String currency;

    // Getters and setters
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public long getUnitAmount() {
        return unitAmount;
    }
    public void setUnitAmount(long unitAmount) {
        this.unitAmount = unitAmount;
    }
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }
}