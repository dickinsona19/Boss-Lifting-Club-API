package com.BossLiftingClub.BossLifting.Stripe.RequestsAndResponses;

public class ProductRequest {
    private String name;
    private long unitAmount;
    private String interval;

    // Default constructor
    public ProductRequest() {}

    // Parameterized constructor
    public ProductRequest(String name, long unitAmount, String interval) {
        this.name = name;
        this.unitAmount = unitAmount;
        this.interval = interval;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getUnitAmount() { return unitAmount; }
    public void setUnitAmount(long unitAmount) { this.unitAmount = unitAmount; }
    public String getInterval() { return interval; }
    public void setInterval(String interval) { this.interval = interval; }
}