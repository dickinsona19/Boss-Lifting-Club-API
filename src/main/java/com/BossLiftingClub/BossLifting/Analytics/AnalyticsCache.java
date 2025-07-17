package com.BossLiftingClub.BossLifting.Analytics;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;


import java.time.LocalDateTime;

@Entity
public class AnalyticsCache {

    @Id
    private String userType;

    @Column(columnDefinition = "TEXT")
    private String analyticsData;

    private LocalDateTime lastUpdated;

    // Getters and setters
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getAnalyticsData() { return analyticsData; }
    public void setAnalyticsData(String analyticsData) { this.analyticsData = analyticsData; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}