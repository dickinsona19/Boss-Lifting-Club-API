package com.BossLiftingClub.BossLifting.Stripe;

import com.BossLiftingClub.BossLifting.Stripe.RequestsAndResponses.*;
import com.BossLiftingClub.BossLifting.User.*;
import com.BossLiftingClub.BossLifting.User.Membership.Membership;
import com.BossLiftingClub.BossLifting.User.Membership.MembershipRepository;
import com.BossLiftingClub.BossLifting.User.UserTitles.UserTitles;
import com.BossLiftingClub.BossLifting.User.UserTitles.UserTitlesRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class StripeController {
    private final StripeService stripeService;
    private final String webhookSecret;
    private final UserService userService;
    private final UserTitlesRepository userTitlesRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final String webhookSubscriptionSecret;
    public StripeController(UserService userService, StripeService stripeService, @Value("${stripe.webhook.secret}") String webhookSecret, @Value("${stripe.webhook.subscriptionSecret}") String webhookSubscriptionSecret, UserTitlesRepository userTitlesRepository,MembershipRepository membershipRepository, UserRepository userRepository) {
        this.stripeService = stripeService;
        this.webhookSecret = webhookSecret;
        this.userService = userService;
        this.userTitlesRepository = userTitlesRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.webhookSubscriptionSecret = webhookSubscriptionSecret;
    }


    @PostMapping("/fullSetupUser")
    public ResponseEntity<Map<String, String>> createUserAndCheckout(@RequestBody UserRequest userRequest) {
        try {
            // Step 1: Check if phone number already exists
            Optional<User> existingUser = userService.getUserByPhoneNumber(userRequest.getPhoneNumber());
            if (existingUser != null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "PhoneNumber already in use");
                return ResponseEntity.badRequest().body(errorResponse); // 400 Bad Request
            }
            User user = new User();
            user.setFirstName(userRequest.getFirstName());
            user.setLastName(userRequest.getLastName());
            user.setPhoneNumber(userRequest.getPhoneNumber());
            user.setPassword(userRequest.getPassword());
            user.setIsInGoodStanding(false);
            user = userService.save(user);

            // 2. Create Stripe customer and link to user
            String customerId = stripeService.createCustomer(
                    null,
                    userRequest.getFirstName() + " " + userRequest.getLastName(),
                    null
            );
            user.setUserStripeMemberId(customerId);
            userService.save(user);

            // 3. Create Checkout session with Product IDs
            String sessionId = stripeService.createCheckoutSession(
                    customerId,
                    "prod_Rq9MIEhW96cwii",    // Replace with your actual activation Product ID
                    "prod_RqBa9OjabJeM0X", // Replace with your actual subscription Product ID
                    "http://localhost:5173/success",
                    "http://localhost:5173/cancel"
            );

            // 4. Return session ID
            Map<String, String> response = new HashMap<>();
            response.put("sessionId", sessionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed: " + e.getMessage()));
        }
    }
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) throws Exception {
        System.out.println(payload);
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            // Handle checkout.session.completed (successful payment method setup)
            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                if (session == null) {
                    return ResponseEntity.status(400).body("Invalid session data");
                }

                String customerId = session.getCustomer();
                String mode = session.getMode();

                if ("setup".equals(mode)) {
                    // Handle setup mode (from /signupWithCard)
                    String setupIntentId = session.getSetupIntent();
                    if (setupIntentId == null) {
                        return ResponseEntity.status(400).body("No setup intent found in session");
                    }

                    SetupIntent setupIntent = SetupIntent.retrieve(setupIntentId);
                    String paymentMethodId = setupIntent.getPaymentMethod();
                    if (paymentMethodId != null) {
                        // Payment method provided, attach it and proceed
                        stripeService.attachPaymentMethod(customerId, paymentMethodId);
                        System.out.println("Payment method " + paymentMethodId + " set as default for customer " + customerId);

                        // Create and save user
                        User user = createUserFromSession(session, customerId);
                        System.out.println("User created with ID: " + user.getId() + " for customer: " + customerId);

                        // Create subscriptions (assuming this method exists from previous request)
                        createSubscriptions(customerId, paymentMethodId, 89.99);

                    } else {
                        // No payment method provided, delete the Stripe customer
                        System.out.println("No payment method attached in setup intent: " + setupIntentId);
                        stripeService.deleteCustomer(customerId);
                        System.out.println("Deleted Stripe customer " + customerId + " due to no payment method.");
                        return ResponseEntity.status(400).body("No payment method found in setup intent");
                    }
                }
            }

            // Handle checkout.session.expired (user canceled or session timed out)
            if ("checkout.session.expired".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                if (session == null) {
                    return ResponseEntity.status(400).body("Invalid session data");
                }

                String customerId = session.getCustomer();
                stripeService.deleteCustomer(customerId);
                System.out.println("Checkout session expired, deleted Stripe customer: " + customerId);
            }

            return ResponseEntity.ok("Webhook handled");
        } catch (StripeException e) {
            e.printStackTrace();
            System.out.println("Webhook error: " + e.getMessage());
            return ResponseEntity.status(500).body("Webhook error: " + e.getMessage());
        }
    }

    private User createUserFromSession(Session session, String customerId) throws Exception {
        Map<String, String> metadata = session.getMetadata();
        User user = new User();
        user.setFirstName(metadata.get("firstName"));
        user.setLastName(metadata.get("lastName"));
        user.setPhoneNumber(metadata.get("phoneNumber"));
        user.setPassword(metadata.get("password"));
        if (metadata.get("referredUserId") != null) {
            User referrer = userRepository.findById(Long.valueOf(metadata.get("referredUserId")))
                    .orElseThrow(() -> new RuntimeException("Referred User not found in database"));
            user.setReferredBy(referrer);
        }

        user.setIsInGoodStanding(false); // Still false until payment succeeds later
        UserTitles foundingUserTitle = userTitlesRepository.findByTitle(metadata.get("userTitle"))
                .orElseThrow(() -> new RuntimeException("Founding user title not found in database"));
        Membership membership = membershipRepository.findByName(metadata.get("membership"))
                .orElseThrow(() -> new RuntimeException("Membership not found in database"));
        user.setMembership(membership);
        user.setLockedInRate(membership.getPrice());
        user.setUserTitles(foundingUserTitle);
        user.setUserStripeMemberId(customerId);
        System.out.println("userLog: " + user);
        System.out.println("getReferredMembersDto: " + user.getReferredMembersDto());
        userService.save(user);
        return user;
    }



    @PostMapping("/signupWithCard")
    public ResponseEntity<Map<String, String>> signupWithCard(@RequestBody UserRequest userRequest) {
        try {
            // Step 1: Check if phone number already exists
            Optional<User> existingUser = userService.getUserByPhoneNumber(userRequest.getPhoneNumber());
            if (existingUser.isPresent()) {
                System.out.println("Existing user found: " + existingUser.get());
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "PhoneNumber already in use");
                return ResponseEntity.badRequest().body(errorResponse); // 400 Bad Request
            }

            // Step 2: Fetch the "founding_user" title and membership from the database
            UserTitles foundingUserTitle = userTitlesRepository.findByTitle("Founding User")
                    .orElseThrow(() -> new RuntimeException("Founding user title not found in database"));
            Membership membership = membershipRepository.findByName(userRequest.getMembershipName())
                    .orElseThrow(() -> new RuntimeException("Membership not found in database"));

            // Step 3: Create Stripe customer (we'll clean up in webhook if no payment info)
            String customerId = stripeService.createCustomer(
                    null, // Email optional
                    userRequest.getFirstName() + " " + userRequest.getLastName(),
                    null  // No payment method yet
            );

            // Step 4: Store user data in metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("firstName", userRequest.getFirstName());
            metadata.put("lastName", userRequest.getLastName());
            metadata.put("phoneNumber", userRequest.getPhoneNumber());
            metadata.put("password", userRequest.getPassword()); // Consider hashing if sensitive
            metadata.put("userTitle", foundingUserTitle.getTitle());
            metadata.put("membership", membership.getName());
            if (userRequest.getReferralId() != null) {
                String referredId = userRequest.getReferralId().toString();
                metadata.put("referredUserId", referredId);
            }

            // Step 5: Create Checkout session in setup mode with metadata
            String sessionId = stripeService.createSetupCheckoutSessionWithMetadata(
                    customerId,
                    "https://www.cltliftingclub.com/success",
                    "https://www.cltliftingclub.com/cancel",
                    metadata
            );

            // Step 6: Return session ID to frontend
            Map<String, String> response = new HashMap<>();
            response.put("sessionId", sessionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed: " + e.getMessage()));
        }
    }
    @PostMapping("/api/migrate-subscriptions")
    public ResponseEntity<String> migrateSubscriptions() {
        try {
            // Retrieve all users using userService.findAll()
            List<User> users = userService.findAll();
            int processed = 0;
            int failed = 0;

            for (User user : users) {
                String customerId = user.getUserStripeMemberId();
                double membershipPrice = 89.99;
                if (customerId == null || customerId.isEmpty()) {
                    System.err.println("Skipping user ID " + user.getId() + ": No Stripe Customer ID");
                    failed++;
                    continue;
                }
                try {
                    // Retrieve default payment method
                    Customer customer = Customer.retrieve(customerId);
                    String paymentMethodId = customer.getInvoiceSettings().getDefaultPaymentMethod();
                    if (paymentMethodId == null) {
                        System.err.println("Skipping user ID " + user.getId() + ": No default payment method for customer " + customerId);
                        failed++;
                        continue;
                    }

                    // Delete existing subscriptions
                    SubscriptionCollection subscriptions = Subscription.list(
                            SubscriptionListParams.builder()
                                    .setCustomer(customerId)
                                    .build()
                    );
                    for (Subscription sub : subscriptions.getData()) {
                        if ("active".equals(sub.getStatus()) || "trialing".equals(sub.getStatus())) {
                            sub.cancel();
                            System.out.println("Canceled existing subscription " + sub.getId() + " for customer " + customerId);
                        }
                    }

                    // Apply new subscriptions
                    createSubscriptions(customerId, paymentMethodId, membershipPrice);
                    System.out.println("Successfully migrated subscriptions for user ID " + user.getId() + ", customer " + customerId);
                    processed++;
                } catch (StripeException e) {
                    System.err.println("Failed to migrate subscriptions for user ID " + user.getId() + ", customer " + customerId + ": " + e.getMessage());
                    failed++;
                }
            }

            String response = String.format("Migration completed: %d users processed successfully, %d failed", processed, failed);
            System.out.println(response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            String error = "Migration failed: " + e.getMessage();
            System.err.println(error);
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/StripeSubscriptionHandler")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request,
                                                      @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            // Read the raw body as bytes
            String payload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Verify webhook signature
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSubscriptionSecret);

            // Deserialize the event data object
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            if (!dataObjectDeserializer.getObject().isPresent()) {
                return new ResponseEntity<>("Invalid event data", HttpStatus.BAD_REQUEST);
            }

            String eventType = event.getType();

            // Handle invoice.paid (transfer 4% fee to Connected Account)
            if ("invoice.paid".equals(event.getType())) {
                Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
                if (invoice != null && invoice.getSubscription() != null) {
                    // Determine the 4% fee based on subscription price (from metadata or Price IDs)
                    Subscription subscription = Subscription.retrieve(invoice.getSubscription());
                    long feeCents = 0;
                    for (SubscriptionItem item : subscription.getItems().getData()) {
                        Price price = item.getPrice();
                        String priceId = price.getId();
                        // Map Price IDs to fee amounts (must match fee Price IDs)
                        switch (priceId) {
                            case "price_1R6aIfGHcVHSTvgIlwN3wmyD":
                            case "price_1RF4FpGHcVHSTvgIKM8Jilwl":
                                feeCents = 360; // $3.60 for $89.99 main
                                break;
                            case "price_1RF313GHcVHSTvgI4HXgjwOA":
                            case "price_1RF4GlGHcVHSTvgIVojlVrn7":
                                feeCents = 400; // $4.00 for $99.99 main
                                break;
                            case "price_1RF31hGHcVHSTvgIbTnGo4vT":
                            case "price_1RF4IsGHcVHSTvgIYOoYfxb9":
                                feeCents = 440; // $4.40 for $109.99 main
                                break;
                            case "price_1RF30SGHcVHSTvgIpegCzQ0m":
                            case "price_1RF4yDGHcVHSTvgIbRn9gXHJ":
                                feeCents = 240; // $2.40 for $59.99 maintenance
                                break;
                        }
                    }
                    if (feeCents > 0) {
                        TransferCreateParams transferParams = TransferCreateParams.builder()
                                .setAmount(feeCents)
                                .setCurrency("usd")
                                .setDestination("acct_1RDvRj4gikNsBARu")
                                .setSourceTransaction(invoice.getCharge())
                                .build();
                        Transfer.create(transferParams);
                        System.out.println("Transferred 4% fee of " + (feeCents / 100.0) + " to Connected Account for invoice " + invoice.getId());
                    }
                }
            }
            switch (eventType) {
                case "customer.subscription.created":
                case "customer.subscription.updated":
                    Subscription subscription = (Subscription) dataObjectDeserializer.getObject().get();
                    String customerId = subscription.getCustomer();
                    String status = subscription.getStatus();
                    boolean isInGoodStanding = "active".equals(status) || "trialing".equals(status);
                    userService.updateUserAfterPayment(customerId, isInGoodStanding);
                    break;

                case "invoice.paid":
                    com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) dataObjectDeserializer.getObject().get();
                    userService.updateUserAfterPayment(invoice.getCustomer(), true);
                    break;

                case "invoice.payment_failed":
                    com.stripe.model.Invoice failedInvoice = (com.stripe.model.Invoice) dataObjectDeserializer.getObject().get();
                    userService.updateUserAfterPayment(failedInvoice.getCustomer(), false);
                    break;

                case "customer.subscription.deleted":
                    Subscription deletedSubscription = (Subscription) dataObjectDeserializer.getObject().get();
                    userService.updateUserAfterPayment(deletedSubscription.getCustomer(), false);
                    break;

                default:
                    System.out.println("Unhandled event type: " + eventType);
            }

            return new ResponseEntity<>("Webhook processed", HttpStatus.OK);
        } catch (SignatureVerificationException e) {
            return new ResponseEntity<>("Invalid signature", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Webhook error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    private void createSubscriptions(String customerId, String paymentMethodId, double membershipPrice) throws StripeException {
        // Determine billing cycle anchor for main subscription based on current date
        LocalDate currentDate = LocalDate.now(ZoneId.of("UTC"));
        LocalDate anchorDate = LocalDate.of(2025, 4, 26); // April 26, 2025
        long mainAnchorDay;
        long mainAnchorMonth;

        if (currentDate.isBefore(anchorDate)) {
            mainAnchorDay = 26L;
            mainAnchorMonth = 4L; // April
            System.out.println("Setting main subscription billing cycle anchor to April 26, 2025 with trial until April 26, 2025");
        } else {
            mainAnchorDay = currentDate.getDayOfMonth();
            mainAnchorMonth = currentDate.getMonthValue();
            System.out.println("Setting main subscription billing cycle anchor to current date: " + currentDate);
        }

        // Map membershipPrice to Price IDs
        String mainPriceId;
        String mainFeePriceId;
        switch (Double.toString(membershipPrice)) {
            case "89.99":
                mainPriceId = "price_1R6aIfGHcVHSTvgIlwN3wmyD"; // Replace with actual Price ID
                mainFeePriceId = "price_1RF4FpGHcVHSTvgIKM8Jilwl"; // $3.60
                break;
            case "99.99":
                mainPriceId = "price_1RF313GHcVHSTvgI4HXgjwOA"; // Replace with actual Price ID
                mainFeePriceId = "price_1RF4GlGHcVHSTvgIVojlVrn7"; // $4.;00
                break;
            case "109.99":
                mainPriceId = "price_1RF31hGHcVHSTvgIbTnGo4vT"; // Replace with actual Price ID
                mainFeePriceId = "price_1RF4IsGHcVHSTvgIYOoYfxb9"; // $4.40
                break;
            default:
                throw new IllegalArgumentException("Invalid membership price: " + membershipPrice);
        }

        // Create main subscription with fee as a recurring item
        SubscriptionCreateParams.Builder subscriptionParamsBuilder = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice(mainPriceId)
                        .build())
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice(mainFeePriceId)
                        .build())
                .setDefaultPaymentMethod(paymentMethodId)
                .setBillingCycleAnchorConfig(
                        SubscriptionCreateParams.BillingCycleAnchorConfig.builder()
                                .setDayOfMonth(mainAnchorDay)
                                .setMonth(mainAnchorMonth)
                                .build()
                )
                .addDefaultTaxRate("txr_1RF33tGHcVHSTvgIzTwKENXt"); // 7.5% tax rate ID

        if (currentDate.isBefore(anchorDate)) {
            subscriptionParamsBuilder.setTrialEnd(1745625600L); // April 26, 2025, 00:00 UTC
            System.out.println("Main subscription trial set with 4% fee recurring post-trial");
        } else {
            System.out.println("Main subscription with immediate charge including recurring 4% fee (transferred via invoice.paid webhook)");
        }

        try {
            Subscription subscription = Subscription.create(subscriptionParamsBuilder.build());
            System.out.println("Main subscription created with ID: " + subscription.getId() + " with billing cycle anchor: " + mainAnchorMonth + "/" + mainAnchorDay + "/2025");
        } catch (StripeException e) {
            System.err.println("Failed to create main subscription: " + e.getMessage());
            throw e;
        }

        // Determine billing cycle for maintenance subscription
        LocalDate januaryBilling = LocalDate.of(currentDate.getYear(), 1, 1);
        LocalDate julyBilling = LocalDate.of(currentDate.getYear(), 7, 1);
        boolean isBeforeJuly = currentDate.isBefore(julyBilling) || currentDate.equals(julyBilling);
        LocalDate previousBillingDate = isBeforeJuly ? januaryBilling : julyBilling;
        long nextBillingMonth = isBeforeJuly ? 7L : 1L; // July 1 or January 1
        long nextBillingDay = 1L;
        int nextBillingYear = isBeforeJuly ? currentDate.getYear() : currentDate.getYear() + 1;

        // Create maintenance subscription with fee as a recurring item
        SubscriptionCreateParams.Builder maintenanceParamsBuilder = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice("price_1RF30SGHcVHSTvgIpegCzQ0m")
                        .build())
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice("price_1RF4yDGHcVHSTvgIbRn9gXHJ") // Replace with actual fee price ID
                        .build())
                .setDefaultPaymentMethod(paymentMethodId)
                .setProrationBehavior(SubscriptionCreateParams.ProrationBehavior.NONE)
                .addDefaultTaxRate("txr_1RF33tGHcVHSTvgIzTwKENXt")
                .addExpand("schedule");

        if (currentDate.isBefore(anchorDate)) {
            maintenanceParamsBuilder
                    .setTrialEnd(1745625600L)
                    .setBillingCycleAnchorConfig(
                            SubscriptionCreateParams.BillingCycleAnchorConfig.builder()
                                    .setDayOfMonth(nextBillingDay)
                                    .setMonth(nextBillingMonth)
                                    .build()
                    );
            System.out.println("Setting maintenance subscription trial until April 26, 2025, with billing cycle anchor to " + nextBillingMonth + "/" + nextBillingDay + "/" + nextBillingYear + " (full $59.99 + tax + recurring 4% fee)");
        } else {
            System.out.println("Setting maintenance subscription billing cycle anchor to " + nextBillingMonth + "/" + nextBillingDay + "/" + nextBillingYear + " (full $59.99 + tax + recurring 4% fee charged immediately, transferred via invoice.paid webhook)");
        }

        try {
            Subscription maintenanceSubscription = Subscription.create(maintenanceParamsBuilder.build());
            System.out.println("Maintenance subscription created with ID: " + maintenanceSubscription.getId());
        } catch (StripeException e) {
            System.err.println("Failed to create maintenance subscription: " + e.getMessage());
            throw e;
        }
    }
}