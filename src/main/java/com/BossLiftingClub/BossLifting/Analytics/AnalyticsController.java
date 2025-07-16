package com.BossLiftingClub.BossLifting.Analytics;

import com.BossLiftingClub.BossLifting.User.User;
import com.BossLiftingClub.BossLifting.User.UserRepository;
import com.stripe.Stripe;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionCollection;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import com.stripe.param.SubscriptionListParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {


    @Autowired
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);
    public AnalyticsController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @GetMapping
    public AnalyticsResponse getAnalytics(@RequestParam String userType) {
        try {
            // Define Price IDs for categorization
            Map<String, String> priceIds = new HashMap<>();
            priceIds.put("founder", "price_1R6aIfGHcVHSTvgIlwN3wmyD");
            priceIds.put("monthly", "price_1RF313GHcVHSTvgI4HXgjwOA");
            priceIds.put("annual", "price_1RJJuTGHcVHSTvgI2pVN6hfx");
            String ignoredPriceId = "price_1RF30SGHcVHSTvgIpegCzQ0m";

            // Fetch all users from existing UserRepository
            List<User> users = userRepository.findAll();
            List<Subscription> relevantSubscriptions = new ArrayList<>();

            // Define time periods for monthly comparison
            LocalDate now = LocalDate.now();
            LocalDate startOfThisMonth = now.withDayOfMonth(1);
            LocalDate startOfLastMonth = startOfThisMonth.minusMonths(1);
            long thisMonthStartEpoch = startOfThisMonth.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
            long lastMonthStartEpoch = startOfLastMonth.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
            long lastMonthEndEpoch = startOfThisMonth.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() - 1;

            // Fetch all subscriptions for each user
            for (User user : users) {
                if (user.getUserStripeMemberId() == null) {
                    logger.warn("Skipping user with ID {}: stripeCustomerId is null", user.getId());
                    continue;
                }

                try {
                    // Query all subscriptions for the user’s stripeCustomerId
                    SubscriptionListParams params = SubscriptionListParams.builder()
                            .setCustomer(user.getUserStripeMemberId())
                            .build();

                    SubscriptionCollection subscriptions = Subscription.list(params);
                    for (Subscription sub : subscriptions.autoPagingIterable()) {
                        try {
                            boolean isRelevant = false;
                            SubscriptionItemCollection items = sub.getItems();
                            for (SubscriptionItem item : items.getData()) {
                                String priceId = item.getPrice().getId();
                                if (!priceId.equals(ignoredPriceId)) {
                                    isRelevant = true;
                                    break;
                                }
                            }
                            if (isRelevant) {
                                relevantSubscriptions.add(sub);
                            }
                        } catch (Exception e) {
                            logger.error("Error processing subscription {} for user {}: {}", sub.getId(), user.getId(), e.getMessage());
                            continue; // Skip problematic subscription
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error fetching subscriptions for user {}: {}", user.getId(), e.getMessage());
                    continue; // Skip problematic user
                }
            }

            // Filter subscriptions by user type
            List<Subscription> filteredSubscriptions = new ArrayList<>();
            if (!userType.equals("all")) {
                String targetPriceId = priceIds.get(userType);
                for (Subscription sub : relevantSubscriptions) {
                    try {
                        for (SubscriptionItem item : sub.getItems().getData()) {
                            String priceId = item.getPrice().getId();
                            if (userType.equals("misc")) {
                                if (!priceIds.containsValue(priceId) && !priceId.equals(ignoredPriceId)) {
                                    filteredSubscriptions.add(sub);
                                    break;
                                }
                            } else if (priceId.equals(targetPriceId)) {
                                filteredSubscriptions.add(sub);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error filtering subscription {}: {}", sub.getId(), e.getMessage());
                        continue; // Skip problematic subscription
                    }
                }
            } else {
                filteredSubscriptions.addAll(relevantSubscriptions);
            }

            // Calculate analytics metrics
            double totalRevenueThisMonth = 0;
            double totalRevenueLastMonth = 0;
            int userCount = 0;
            Map<String, UserTypeData> userTypeBreakdown = new HashMap<>();
            Map<String, Double> weeklyRevenueThisMonth = new HashMap<>();
            Map<String, Double> weeklyRevenueLastMonth = new HashMap<>();
            String[] weeks = {"Week 1", "Week 2", "Week 3", "Week 4"};

            // Initialize weekly data
            for (String week : weeks) {
                weeklyRevenueThisMonth.put(week, 0.0);
                weeklyRevenueLastMonth.put(week, 0.0);
            }

            for (Subscription sub : filteredSubscriptions) {
                try {
                    userCount++;
                    for (SubscriptionItem item : sub.getItems().getData()) {
                        String priceId = item.getPrice().getId();
                        if (priceId.equals(ignoredPriceId)) continue;

                        // Categorize as misc if not a known price ID
                        String subscriptionType = priceIds.entrySet().stream()
                                .filter(entry -> entry.getValue().equals(priceId))
                                .map(Map.Entry::getKey)
                                .findFirst()
                                .orElse("misc");

                        long unitAmount = item.getPrice().getUnitAmount();
                        double amountInDollars = unitAmount / 100.0;

                        // Update user type breakdown
                        UserTypeData typeData = userTypeBreakdown.getOrDefault(subscriptionType, new UserTypeData());
                        typeData.count++;
                        typeData.revenue += amountInDollars;
                        userTypeBreakdown.put(subscriptionType, typeData);

                        // Determine the subscription's billing period
                        long created = sub.getCreated();
                        String interval = item.getPrice().getRecurring().getInterval();
                        long periodStart = sub.getCurrentPeriodStart();
                        long periodEnd = sub.getCurrentPeriodEnd();

                        // Assign revenue to this month or last month
                        if (periodStart >= thisMonthStartEpoch) {
                            totalRevenueThisMonth += amountInDollars;
                            // Fix weekIndex to prevent negative values
                            long daysSinceMonthStart = (created - thisMonthStartEpoch) / (24 * 3600);
                            int weekIndex = Math.min(Math.max((int) (daysSinceMonthStart / 7), 0), 3);
                            weeklyRevenueThisMonth.put(weeks[weekIndex], weeklyRevenueThisMonth.get(weeks[weekIndex]) + amountInDollars);
                        } else if (periodStart >= lastMonthStartEpoch && periodEnd <= lastMonthEndEpoch) {
                            totalRevenueLastMonth += amountInDollars;
                            long daysSinceLastMonthStart = (created - lastMonthStartEpoch) / (24 * 3600);
                            int weekIndex = Math.min(Math.max((int) (daysSinceLastMonthStart / 7), 0), 3);
                            weeklyRevenueLastMonth.put(weeks[weekIndex], weeklyRevenueLastMonth.get(weeks[weekIndex]) + amountInDollars);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing subscription {}: {}", sub.getId(), e.getMessage());
                    continue; // Skip problematic subscription
                }
            }

            // Calculate metrics
            double averageRevenue = userCount > 0 ? totalRevenueThisMonth / userCount : 0;
            double percentageChange = totalRevenueLastMonth > 0 ?
                    ((totalRevenueThisMonth - totalRevenueLastMonth) / totalRevenueLastMonth) * 100 : 0;

            // Prepare response
            AnalyticsResponse response = new AnalyticsResponse();
            response.setTotalRevenue(totalRevenueThisMonth);
            response.setUserCount(userCount);
            response.setAverageRevenue(averageRevenue);
            response.setMonthlyComparison(new MonthlyComparison(
                    totalRevenueThisMonth,
                    totalRevenueLastMonth,
                    percentageChange
            ));
            response.setChartData(new ChartData(
                    weeks,
                    weeklyRevenueThisMonth.values().toArray(new Double[0]),
                    weeklyRevenueLastMonth.values().toArray(new Double[0])
            ));
            response.setUserTypeBreakdown(userTypeBreakdown);

            return response;

        } catch (Exception e) {
            logger.error("Unexpected error in getAnalytics: {}", e.getMessage());
            throw new RuntimeException("Error fetching analytics data: " + e.getMessage());
        }
    }

    // DTO classes
    public static class AnalyticsResponse {
        private double totalRevenue;
        private int userCount;
        private double averageRevenue;
        private MonthlyComparison monthlyComparison;
        private ChartData chartData;
        private Map<String, UserTypeData> userTypeBreakdown;

        // Getters and setters
        public double getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
        public int getUserCount() { return userCount; }
        public void setUserCount(int userCount) { this.userCount = userCount; }
        public double getAverageRevenue() { return averageRevenue; }
        public void setAverageRevenue(double averageRevenue) { this.averageRevenue = averageRevenue; }
        public MonthlyComparison getMonthlyComparison() { return monthlyComparison; }
        public void setMonthlyComparison(MonthlyComparison monthlyComparison) { this.monthlyComparison = monthlyComparison; }
        public ChartData getChartData() { return chartData; }
        public void setChartData(ChartData chartData) { this.chartData = chartData; }
        public Map<String, UserTypeData> getUserTypeBreakdown() { return userTypeBreakdown; }
        public void setUserTypeBreakdown(Map<String, UserTypeData> userTypeBreakdown) { this.userTypeBreakdown = userTypeBreakdown; }
    }

    public static class MonthlyComparison {
        private double thisMonth;
        private double lastMonth;
        private double percentageChange;

        public MonthlyComparison(double thisMonth, double lastMonth, double percentageChange) {
            this.thisMonth = thisMonth;
            this.lastMonth = lastMonth;
            this.percentageChange = percentageChange;
        }

        // Getters
        public double getThisMonth() { return thisMonth; }
        public double getLastMonth() { return lastMonth; }
        public double getPercentageChange() { return percentageChange; }
    }

    public static class ChartData {
        private String[] labels;
        private Double[] thisMonth;
        private Double[] lastMonth;

        public ChartData(String[] labels, Double[] thisMonth, Double[] lastMonth) {
            this.labels = labels;
            this.thisMonth = thisMonth;
            this.lastMonth = lastMonth;
        }

        // Getters
        public String[] getLabels() { return labels; }
        public Double[] getThisMonth() { return thisMonth; }
        public Double[] getLastMonth() { return lastMonth; }
    }

    public static class UserTypeData {
        private int count;
        private double revenue;

        // Getters and setters
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public double getRevenue() { return revenue; }
        public void setRevenue(double revenue) { this.revenue = revenue; }
    }
}