package com.BossLiftingClub.BossLifting.Analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AnalyticsCacheRepository extends JpaRepository<AnalyticsCache, String> {
    Optional<AnalyticsCache> findById(String userType);
}