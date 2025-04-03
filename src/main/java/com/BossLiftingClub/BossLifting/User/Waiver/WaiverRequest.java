package com.BossLiftingClub.BossLifting.User.Waiver;

public class WaiverRequest {
    private Long userId;
    private String signature; // Base64-encoded string

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}