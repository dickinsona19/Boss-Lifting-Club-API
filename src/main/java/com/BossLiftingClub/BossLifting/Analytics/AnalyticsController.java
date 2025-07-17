package com.BossLiftingClub.BossLifting.Analytics;

import com.BossLiftingClub.BossLifting.User.User;
import com.BossLiftingClub.BossLifting.User.UserRepository;
import com.stripe.Stripe;
import com.stripe.model.*;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.InvoiceListParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);


    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final AnalyticsCacheRepository analyticsCacheRepository;

    @Autowired
    private final ObjectMapper objectMapper;

    public AnalyticsController(UserRepository userRepository, AnalyticsCacheRepository analyticsCacheRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.analyticsCacheRepository = analyticsCacheRepository;
        this.objectMapper = objectMapper;
    }


    @GetMapping
    public AnalyticsResponse getAnalytics(@RequestParam String userType) {
        try {
            // Validate userType
            if (!userType.equals("all") && !userType.equals("founder") && !userType.equals("monthly") && !userType.equals("annual") && !userType.equals("misc")) {
                throw new IllegalArgumentException("Invalid userType: " + userType);
            }

            // Check cache first
            AnalyticsCache cache = analyticsCacheRepository.findById(userType).orElse(null);
            if (cache != null && cache.getLastUpdated() != null &&
                    cache.getLastUpdated().isAfter(LocalDateTime.now().minusHours(12))) {
                logger.info("Serving cached analytics data for userType={}", userType);
                return objectMapper.readValue(cache.getAnalyticsData(), AnalyticsResponse.class);
            }

            // Calculate live data if cache is empty or stale
            AnalyticsResponse response = calculateAnalytics(userType);

            // Save to cache
            AnalyticsCache newCache = new AnalyticsCache();
            newCache.setUserType(userType);
            newCache.setAnalyticsData(objectMapper.writeValueAsString(response));
            newCache.setLastUpdated(LocalDateTime.now());
            analyticsCacheRepository.save(newCache);

            logger.info("Calculated and cached analytics data for userType={}", userType);
            return response;
        } catch (Exception e) {
            logger.error("Unexpected error in getAnalytics for userType={}: {}", userType, e.getMessage(), e);
            throw new RuntimeException("Error fetching analytics data: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 0,12 * * ?") // Run at 00:00 and 12:00 daily
    public void updateAnalyticsCache() {
        try {
            logger.info("Starting scheduled analytics cache update");
            String[] userTypes = {"all", "founder", "monthly", "annual", "misc"};
            for (String userType : userTypes) {
                AnalyticsResponse response = calculateAnalytics(userType);
                AnalyticsCache cache = new AnalyticsCache();
                cache.setUserType(userType);
                cache.setAnalyticsData(objectMapper.writeValueAsString(response));
                cache.setLastUpdated(LocalDateTime.now());
                analyticsCacheRepository.save(cache);
                logger.info("Cached analytics data for userType={}", userType);
            }
            logger.info("Completed scheduled analytics cache update");
        } catch (Exception e) {
            logger.error("Error updating analytics cache: {}", e.getMessage(), e);
        }
    }

    private AnalyticsResponse calculateAnalytics(String userType) {
        try {
            // Define Price IDs for categorization
            Map<String, String> priceIds = new HashMap<>();
            priceIds.put("founder", "price_1R6aIfGHcVHSTvgIlwN3wmyD");
            priceIds.put("monthly", "price_1RF313GHcVHSTvgI4HXgjwOA");
            priceIds.put("annual", "price_1RJJuTGHcVHSTvgI2pVN6hfx");
            String ignoredPriceId = "price_1RF30SGHcVHSTvgIpegCzQ0m";

            // Fetch all users from UserRepository
            List<User> users;
            try {
                users = userRepository.findAll();
                logger.info("Fetched {} users from UserRepository", users.size());
            } catch (Exception e) {
                logger.error("Error fetching users: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to fetch users: " + e.getMessage());
            }

            List<Subscription> relevantSubscriptions = new ArrayList<>();

            // Define time periods
            LocalDateTime now = LocalDateTime.now();
            LocalDate startOfThisMonth = now.toLocalDate().withDayOfMonth(1);
            LocalDate endOfThisMonth = startOfThisMonth.plusMonths(1).minusDays(1);
            LocalDate startOfLastMonth = startOfThisMonth.minusMonths(1);
            LocalDate endOfLastMonth = startOfLastMonth.plusMonths(1).minusDays(1);
            long thisMonthStartEpoch = startOfThisMonth.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
            long currentDateEpoch = now.atZone(ZoneId.systemDefault()).toEpochSecond();
            long thisMonthEndEpoch = endOfThisMonth.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond();
            long lastMonthStartEpoch = startOfLastMonth.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
            long lastMonthEndEpoch = endOfLastMonth.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond();

            // Fetch all subscriptions for each user
            for (User user : users) {
                if (user.getUserStripeMemberId() == null) {
                    logger.warn("Skipping user with ID {}: stripeCustomerId is null", user.getId());
                    continue;
                }

                try {
                    SubscriptionListParams params = SubscriptionListParams.builder()
                            .setCustomer(user.getUserStripeMemberId())
                            .build();

                    SubscriptionCollection subscriptions = Subscription.list(params);
                    for (Subscription sub : subscriptions.autoPagingIterable()) {
                        try {
                            boolean isRelevant = false;
                            SubscriptionItemCollection items = sub.getItems();
                            for (SubscriptionItem item : items.getData()) {
                                String priceId = item.getPrice() != null ? item.getPrice().getId() : null;
                                if (priceId == null) {
                                    logger.warn("Skipping subscription item with null price for subscription {} and user {}", sub.getId(), user.getId());
                                    continue;
                                }
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
                            continue;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error fetching subscriptions for user {}: {}", user.getId(), e.getMessage());
                    continue;
                }
            }

            // Filter subscriptions by user type
            List<Subscription> filteredSubscriptions = new ArrayList<>();
            if (!userType.equals("all")) {
                String targetPriceId = priceIds.get(userType);
                for (Subscription sub : relevantSubscriptions) {
                    try {
                        for (SubscriptionItem item : sub.getItems().getData()) {
                            String priceId = item.getPrice() != null ? item.getPrice().getId() : null;
                            if (priceId == null) continue;
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
                        continue;
                    }
                }
            } else {
                filteredSubscriptions.addAll(relevantSubscriptions);
            }

            // Calculate analytics metrics
            double actualRevenueThisMonth = 0;
            double projectedRevenueRestOfMonth = 0;
            double totalRevenueLastMonth = 0;
            int userCount = 0;
            Map<String, UserTypeData> userTypeBreakdown = new HashMap<>();
            Map<String, Double> weeklyRevenueThisMonthActual = new HashMap<>();
            Map<String, Double> weeklyRevenueThisMonthProjected = new HashMap<>();
            Map<String, Double> weeklyRevenueLastMonth = new HashMap<>();
            Map<String, Integer> userTypeCounts = new HashMap<>();
            String[] weeks = {"Week 1", "Week 2", "Week 3", "Week 4"};

            // Initialize weekly data
            for (String week : weeks) {
                weeklyRevenueThisMonthActual.put(week, 0.0);
                weeklyRevenueThisMonthProjected.put(week, 0.0);
                weeklyRevenueLastMonth.put(week, 0.0);
            }

            // Calculate lifetime revenue for userTypeBreakdown
            for (User user : users) {
                if (user.getUserStripeMemberId() == null) continue;

                try {
                    InvoiceListParams invoiceParams = InvoiceListParams.builder()
                            .setCustomer(user.getUserStripeMemberId())
                            .setStatus(InvoiceListParams.Status.PAID)
                            .build();

                    for (Invoice invoice : Invoice.list(invoiceParams).autoPagingIterable()) {
                        try {
                            for (com.stripe.model.InvoiceLineItem line : invoice.getLines().getData()) {
                                String priceId = line.getPrice() != null ? line.getPrice().getId() : null;
                                if (priceId == null || priceId.equals(ignoredPriceId)) continue;

                                String subscriptionType = priceIds.entrySet().stream()
                                        .filter(entry -> entry.getValue().equals(priceId))
                                        .map(Map.Entry::getKey)
                                        .findFirst()
                                        .orElse("misc");

                                if (!userType.equals("all") && !userType.equals(subscriptionType) && !(userType.equals("misc") && !priceIds.containsValue(priceId))) {
                                    continue;
                                }

                                double amountInDollars = line.getAmount() / 100.0;
                                UserTypeData typeData = userTypeBreakdown.getOrDefault(subscriptionType, new UserTypeData());
                                typeData.revenue += amountInDollars;
                                userTypeBreakdown.put(subscriptionType, typeData);
                            }
                        } catch (Exception e) {
                            logger.error("Error processing invoice {} for user {}: {}", invoice.getId(), user.getId(), e.getMessage());
                            continue;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error fetching lifetime invoices for user {}: {}", user.getId(), e.getMessage());
                    continue;
                }
            }

            // Calculate actual revenue this month (invoices paid from start of month to current date)
            for (User user : users) {
                if (user.getUserStripeMemberId() == null) continue;

                try {
                    InvoiceListParams invoiceParams = InvoiceListParams.builder()
                            .setCustomer(user.getUserStripeMemberId())
                            .setStatus(InvoiceListParams.Status.PAID)
                            .setCreated(InvoiceListParams.Created.builder()
                                    .setGte(thisMonthStartEpoch)
                                    .setLte(currentDateEpoch)
                                    .build())
                            .build();

                    for (Invoice invoice : Invoice.list(invoiceParams).autoPagingIterable()) {
                        try {
                            for (com.stripe.model.InvoiceLineItem line : invoice.getLines().getData()) {
                                String priceId = line.getPrice() != null ? line.getPrice().getId() : null;
                                if (priceId == null || priceId.equals(ignoredPriceId)) continue;

                                String subscriptionType = priceIds.entrySet().stream()
                                        .filter(entry -> entry.getValue().equals(priceId))
                                        .map(Map.Entry::getKey)
                                        .findFirst()
                                        .orElse("misc");

                                if (!userType.equals("all") && !userType.equals(subscriptionType) && !(userType.equals("misc") && !priceIds.containsValue(priceId))) {
                                    continue;
                                }

                                double amountInDollars = line.getAmount() / 100.0;
                                actualRevenueThisMonth += amountInDollars;

                                long created = invoice.getCreated();
                                long daysInMonth = (created - thisMonthStartEpoch) / (24 * 3600);
                                int weekIndex = Math.min(Math.max((int) (daysInMonth / 7), 0), 3);
                                weeklyRevenueThisMonthActual.put(weeks[weekIndex], weeklyRevenueThisMonthActual.get(weeks[weekIndex]) + amountInDollars);
                            }
                        } catch (Exception e) {
                            logger.error("Error processing invoice {} for user {}: {}", invoice.getId(), user.getId(), e.getMessage());
                            continue;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error fetching invoices for user {}: {}", user.getId(), e.getMessage());
                    continue;
                }
            }

            // Calculate last month's revenue (invoices paid last month)
            for (User user : users) {
                if (user.getUserStripeMemberId() == null) continue;

                try {
                    InvoiceListParams invoiceParams = InvoiceListParams.builder()
                            .setCustomer(user.getUserStripeMemberId())
                            .setStatus(InvoiceListParams.Status.PAID)
                            .setCreated(InvoiceListParams.Created.builder()
                                    .setGte(lastMonthStartEpoch)
                                    .setLte(lastMonthEndEpoch)
                                    .build())
                            .build();

                    for (Invoice invoice : Invoice.list(invoiceParams).autoPagingIterable()) {
                        try {
                            for (com.stripe.model.InvoiceLineItem line : invoice.getLines().getData()) {
                                String priceId = line.getPrice() != null ? line.getPrice().getId() : null;
                                if (priceId == null || priceId.equals(ignoredPriceId)) continue;

                                String subscriptionType = priceIds.entrySet().stream()
                                        .filter(entry -> entry.getValue().equals(priceId))
                                        .map(Map.Entry::getKey)
                                        .findFirst()
                                        .orElse("misc");

                                if (!userType.equals("all") && !userType.equals(subscriptionType) && !(userType.equals("misc") && !priceIds.containsValue(priceId))) {
                                    continue;
                                }

                                double amountInDollars = line.getAmount() / 100.0;
                                totalRevenueLastMonth += amountInDollars;

                                long created = invoice.getCreated();
                                long daysInMonth = (created - lastMonthStartEpoch) / (24 * 3600);
                                int weekIndex = Math.min(Math.max((int) (daysInMonth / 7), 0), 3);
                                weeklyRevenueLastMonth.put(weeks[weekIndex], weeklyRevenueLastMonth.get(weeks[weekIndex]) + amountInDollars);
                            }
                        } catch (Exception e) {
                            logger.error("Error processing invoice {} for user {}: {}", invoice.getId(), user.getId(), e.getMessage());
                            continue;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error fetching invoices for user {}: {}", user.getId(), e.getMessage());
                    continue;
                }
            }

            // Calculate projected revenue for rest of month (upcoming renewals)
            for (Subscription sub : filteredSubscriptions) {
                try {
                    if (!sub.getStatus().equals("active")) continue;

                    for (SubscriptionItem item : sub.getItems().getData()) {
                        String priceId = item.getPrice() != null ? item.getPrice().getId() : null;
                        if (priceId == null || priceId.equals(ignoredPriceId)) continue;

                        String subscriptionType = priceIds.entrySet().stream()
                                .filter(entry -> entry.getValue().equals(priceId))
                                .map(Map.Entry::getKey)
                                .findFirst()
                                .orElse("misc");

                        if (!userType.equals("all") && !userType.equals(subscriptionType) && !(userType.equals("misc") && !priceIds.containsValue(priceId))) {
                            continue;
                        }

                        userTypeCounts.compute(subscriptionType, (k, v) -> (v == null) ? 1 : v + 1);

                        long unitAmount = item.getPrice().getUnitAmount();
                        double amountInDollars = unitAmount / 100.0;

                        if (sub.getCurrentPeriodEnd() > currentDateEpoch && sub.getCurrentPeriodEnd() <= thisMonthEndEpoch) {
                            projectedRevenueRestOfMonth += amountInDollars;

                            long periodEnd = sub.getCurrentPeriodEnd();
                            long daysInMonth = (periodEnd - thisMonthStartEpoch) / (24 * 3600);
                            int weekIndex = Math.min(Math.max((int) (daysInMonth / 7), 0), 3);
                            weeklyRevenueThisMonthProjected.put(weeks[weekIndex], weeklyRevenueThisMonthProjected.get(weeks[weekIndex]) + amountInDollars);
                        }
                    }
                    userCount++;
                } catch (Exception e) {
                    logger.error("Error processing subscription {}: {}", sub.getId(), e.getMessage());
                    continue;
                }
            }

            // Set user type counts
            userTypeBreakdown.forEach((type, data) -> data.setCount(userTypeCounts.getOrDefault(type, 0)));

            // Calculate total revenue for this month (actual only)
            double totalRevenueThisMonth = actualRevenueThisMonth;

            // Calculate combined total for display
            double combinedTotal = actualRevenueThisMonth + projectedRevenueRestOfMonth;

            // Prepare chart data
            Double[] weeklyRevenueThisMonthActualArray = new Double[4];
            Double[] weeklyRevenueThisMonthProjectedArray = new Double[4];
            for (int i = 0; i < 4; i++) {
                weeklyRevenueThisMonthActualArray[i] = weeklyRevenueThisMonthActual.get(weeks[i]);
                weeklyRevenueThisMonthProjectedArray[i] = weeklyRevenueThisMonthProjected.get(weeks[i]);
            }

            // Calculate metrics
            double percentageChange = totalRevenueLastMonth > 0 ?
                    ((totalRevenueThisMonth - totalRevenueLastMonth) / totalRevenueLastMonth) * 100 : 0;

            // Prepare response
            AnalyticsResponse response = new AnalyticsResponse();
            response.setTotalRevenue(totalRevenueThisMonth);
            response.setUserCount(userCount);
            response.setProjectedRevenue(projectedRevenueRestOfMonth);
            response.setMonthlyComparison(new MonthlyComparison(
                    totalRevenueThisMonth,
                    totalRevenueLastMonth,
                    percentageChange,
                    combinedTotal
            ));
            response.setChartData(new ChartData(
                    weeks,
                    weeklyRevenueThisMonthActualArray,
                    weeklyRevenueThisMonthProjectedArray,
                    weeklyRevenueLastMonth.values().toArray(new Double[0])
            ));
            response.setUserTypeBreakdown(userTypeBreakdown);

            logger.info("Analytics processed successfully for userType={}: actualRevenueThisMonth={}, projectedRevenueRestOfMonth={}, totalRevenueThisMonth={}, combinedTotal={}, lastMonthRevenue={}, userCount={}",
                    userType, actualRevenueThisMonth, projectedRevenueRestOfMonth, totalRevenueThisMonth, combinedTotal, totalRevenueLastMonth, userCount);

            return response;
        } catch (Exception e) {
            logger.error("Error in calculateAnalytics for userType={}: {}", userType, e.getMessage(), e);
            throw new RuntimeException("Error calculating analytics data: " + e.getMessage());
        }
    }

    // DTO classes
    public static class AnalyticsResponse {
        private double totalRevenue;
        private int userCount;
        private double projectedRevenue;
        private MonthlyComparison monthlyComparison;
        private ChartData chartData;
        private Map<String, UserTypeData> userTypeBreakdown;

        public double getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
        public int getUserCount() { return userCount; }
        public void setUserCount(int userCount) { this.userCount = userCount; }
        public double getProjectedRevenue() { return projectedRevenue; }
        public void setProjectedRevenue(double projectedRevenue) { this.projectedRevenue = projectedRevenue; }
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
        private double total;

        public MonthlyComparison(double thisMonth, double lastMonth, double percentageChange, double total) {
            this.thisMonth = thisMonth;
            this.lastMonth = lastMonth;
            this.percentageChange = percentageChange;
            this.total = total;
        }

        public double getThisMonth() { return thisMonth; }
        public double getLastMonth() { return lastMonth; }
        public double getPercentageChange() { return percentageChange; }
        public double getTotal() { return total; }
    }

    public static class ChartData {
        private String[] labels;
        private Double[] thisMonthActual;
        private Double[] thisMonthProjected;
        private Double[] lastMonth;

        public ChartData(String[] labels, Double[] thisMonthActual, Double[] thisMonthProjected, Double[] lastMonth) {
            this.labels = labels;
            this.thisMonthActual = thisMonthActual;
            this.thisMonthProjected = thisMonthProjected;
            this.lastMonth = lastMonth;
        }

        public String[] getLabels() { return labels; }
        public Double[] getThisMonthActual() { return thisMonthActual; }
        public Double[] getThisMonthProjected() { return thisMonthProjected; }
        public Double[] getLastMonth() { return lastMonth; }
    }

    public static class UserTypeData {
        private int count;
        private double revenue;

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public double getRevenue() { return revenue; }
        public void setRevenue(double revenue) { this.revenue = revenue; }
    }
}