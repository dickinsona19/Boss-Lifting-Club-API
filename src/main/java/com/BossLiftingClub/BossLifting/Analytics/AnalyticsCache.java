package com.BossLiftingClub.BossLifting.Analytics;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class AnalyticsCache {

    @Id
    private String cacheKey; // Changed from userType to cacheKey to match the composite key
    private String analyticsData;
    private LocalDateTime lastUpdated;

    // Default constructor
    public AnalyticsCache() {
    }

    // Getters and Setters
    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public String getAnalyticsData() {
        return analyticsData;
    }

    public void setAnalyticsData(String analyticsData) {
        this.analyticsData = analyticsData;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}