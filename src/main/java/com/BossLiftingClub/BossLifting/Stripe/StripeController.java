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
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class StripeController {
    private final StripeService stripeService;
    private final String webhookSecret;
    private final UserService userService;
    private final UserTitlesRepository userTitlesRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    public StripeController(UserService userService, StripeService stripeService, @Value("${stripe.webhook.secret}") String webhookSecret, UserTitlesRepository userTitlesRepository,MembershipRepository membershipRepository, UserRepository userRepository) {
        this.stripeService = stripeService;
        this.webhookSecret = webhookSecret;
        this.userService = userService;
        this.userTitlesRepository = userTitlesRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    // Create Product and Price
    @PostMapping("/products")
    public ResponseEntity<?> createProductAndPrice(
            @RequestBody ProductRequest request) {
        try {
            stripeService.createProductAndPrice(
                    request.getName(),
                    request.getUnitAmount(),
                    request.getInterval()
            );
            return ResponseEntity.ok().body("Product and price created successfully");
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Create Customer
    @PostMapping("/customers")
    public ResponseEntity<?> createCustomer(@RequestBody CustomerRequest request) {
        try {
            String customerId = stripeService.createCustomer(
                    request.getEmail(),
                    request.getFullName(),
                    request.getPaymentMethodId()
            );
            return ResponseEntity.ok().body(new CustomerResponse(customerId));
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    // Create Payment Intent
    @PostMapping("/payment-intents")
    public ResponseEntity<?> createPaymentIntent(@RequestBody PaymentIntentRequest request) {
        try {
            PaymentIntent intent = stripeService.createPaymentIntent(
                    request.getCustomerId(),
                    request.getPriceId()
            );
            return ResponseEntity.ok().body(new PaymentIntentResponse(
                    intent.getId(),
                    intent.getClientSecret()
            ));
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Create Subscription
    @PostMapping("/subscriptions")
    public ResponseEntity<?> createSubscription(@RequestBody SubscriptionRequest request) {
        try {
            Subscription subscription = stripeService.createSubscription(
                    request.getCustomerId(),
                    request.getPriceId()
            );
            return ResponseEntity.ok().body(new SubscriptionResponse(
                    subscription.getId(),
                    subscription.getStatus() // Include status for clarity
            ));
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Attach Payment Method
    @PostMapping("/payment-methods/attach")
    public ResponseEntity<?> attachPaymentMethod(
            @RequestBody PaymentMethodRequest request) {
        try {
            stripeService.attachPaymentMethod(
                    request.getCustomerId(),
                    request.getPaymentMethodId()
            );
            return ResponseEntity.ok().body("Payment method attached successfully");
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Create Founding Member Coupon
    @PostMapping("/coupons/founding-member")
    public ResponseEntity<?> createFoundingMemberCoupon() {
        try {
            String couponId = stripeService.createFoundingMemberCoupon();
            return ResponseEntity.ok().body(new CouponResponse(couponId));
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Create Founding Member Subscription
    @PostMapping("/subscriptions/founding-member")
    public ResponseEntity<?> createFoundingMemberSubscription(
            @RequestBody FoundingMemberSubscriptionRequest request) {
        try {
            Subscription subscription = stripeService.createFoundingMemberSubscription(
                    request.getCustomerId(),
                    request.getPriceId(),
                    request.getCouponId()
            );
            return ResponseEntity.ok().body(new SubscriptionResponse(subscription.getId(), subscription.getStatus()));
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Create One-time Payment Intent
    @PostMapping("/payments")
    public ResponseEntity<?> createPayment(
            @RequestBody PaymentRequest request) {
        try {
            PaymentResponse response = stripeService.createPaymentIntent(request);
            return ResponseEntity.ok().body(response);
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }


    @PostMapping("/charge-product")
    public ResponseEntity<?> chargeCustomerForProduct(@RequestBody ChargeProductRequest request) {
        try {
            PaymentIntent intent = stripeService.chargeCustomerForProduct(
                    request.getCustomerId(),
                    request.getProductId()
            );
            return ResponseEntity.ok().body(new PaymentIntentResponse(
                    intent.getId(),
                    intent.getClientSecret() // Included for consistency, though not needed for off-session
            ));
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
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

                        // Create user from metadata
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
                                .orElseThrow(() -> new RuntimeException("Founding user title not found in database"));
                        user.setMembership(membership);
                        user.setLockedInRate(membership.getPrice());
                        user.setUserTitles(foundingUserTitle);
                        user.setUserStripeMemberId(customerId);
                        System.out.println("userLog: " + user);
                        System.out.println("getReferredMembersDto: " + user.getReferredMembersDto());
                        userService.save(user);

                        System.out.println("User created with ID: " + user.getId() + " for customer: " + customerId);
                        // Create a subscription starting April 26, 2025
                        long trialEndTimestamp = 1745625600L; // UNIX timestamp for April 26, 2025, 00:00 UTC
                        SubscriptionCreateParams subscriptionParams = SubscriptionCreateParams.builder()
                                .setCustomer(customerId)
                                .addItem(SubscriptionCreateParams.Item.builder()
                                        .setPrice("price_1R6II54PBwB8fzGsIhXvOVuT")
                                        .build())
                                .setDefaultPaymentMethod(paymentMethodId)
                                .setTrialEnd(trialEndTimestamp)
                                .setApplicationFeePercent(BigDecimal.valueOf(4.0)) // 4% fee applied to every charge
                                .setTransferData(SubscriptionCreateParams.TransferData.builder()

                                        .build())
                                .build();

                        Subscription subscription = Subscription.create(subscriptionParams);
                        System.out.println("Subscription created with ID: " + subscription.getId() + " starting on April 12, 2025");
                        Product maintenanceProduct = Product.create(
                                ProductCreateParams.builder()
                                        .setName("Maintenance Fee")
                                        .setType(ProductCreateParams.Type.SERVICE)
                                        .build()
                        );
                        PriceCreateParams maintenancePriceParams = PriceCreateParams.builder()
                                .setCurrency("usd")
                                .setUnitAmount(5999L)
                                .setRecurring(
                                        PriceCreateParams.Recurring.builder()
                                                .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
                                                .setIntervalCount(6L) // Every 6 months
                                                .build()
                                )
                                .setProduct(maintenanceProduct.getId())
                                .build();
                        Price maintenancePrice = Price.create(maintenancePriceParams);
                        SubscriptionCreateParams maintenanceParams = SubscriptionCreateParams.builder()
                                .setCustomer(customerId)
                                .addItem(SubscriptionCreateParams.Item.builder()
                                        .setPrice(maintenancePrice.getId())
                                        .build())
                                .setDefaultPaymentMethod(paymentMethodId)
                                .setApplicationFeePercent(BigDecimal.valueOf(4.0))
                                .setTransferData(SubscriptionCreateParams.TransferData.builder()

                                        .build())
                                .setBillingCycleAnchorConfig(
                                        SubscriptionCreateParams.BillingCycleAnchorConfig.builder()
                                                .setDayOfMonth(1L)
                                                .setMonth(1L) // January
                                                .build()
                                )
                                .addExpand("schedule") // For debugging billing cycle
                                .build();

                        Subscription maintenanceSubscription = Subscription.create(maintenanceParams);
                    } else {
                        // No payment method provided, delete the Stripe customer
                        System.out.println("No payment method attached in setup intent: " + setupIntentId);
                        stripeService.deleteCustomer(customerId);
                        System.out.println("Deleted Stripe customer " + customerId + " due to no payment method.");
                        return ResponseEntity.status(400).body("No payment method found in setup intent");
                    }

                }

//                else if ("subscription".equals(mode)) {
//                    // Handle subscription mode (unchanged)
//                    String subscriptionId = session.getSubscription();
//                    if (subscriptionId == null) {
//                        return ResponseEntity.status(400).body("No subscription found in session");
//                    }
//
//                    Subscription subscription = Subscription.retrieve(subscriptionId);
//                    String invoiceId = subscription.getLatestInvoice();
//                    if (invoiceId == null) {
//                        return ResponseEntity.status(400).body("No invoice found for subscription");
//                    }
//                    Invoice invoice = Invoice.retrieve(invoiceId);
//                    String paymentIntentId = invoice.getPaymentIntent();
//
//                    if (paymentIntentId != null) {
//                        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
//                        String paymentMethodId = paymentIntent.getPaymentMethod();
//                        stripeService.attachPaymentMethod(customerId, paymentMethodId);
//                    } else {
//                        String defaultPaymentMethodId = subscription.getDefaultPaymentMethod();
//                        if (defaultPaymentMethodId != null) {
//                            stripeService.attachPaymentMethod(customerId, defaultPaymentMethodId);
//                        } else {
//                            System.out.println("No payment method found for subscription: " + subscriptionId);
//                        }
//                    }
//
//                    userService.updateUserAfterPayment(customerId);
//                }
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
    @PostMapping("/StripeSubscriptionHandler")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        try {
            // Verify webhook signature
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            // Deserialize the event data object
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            if (!dataObjectDeserializer.getObject().isPresent()) {
                return new ResponseEntity<>("Invalid event data", HttpStatus.BAD_REQUEST);
            }

            // Handle specific events
            String eventType = event.getType();
            switch (eventType) {
                case "customer.subscription.created":
                case "customer.subscription.updated":
                    Subscription subscription = (Subscription) dataObjectDeserializer.getObject().get();
                    String customerId = subscription.getCustomer();
                    String status = subscription.getStatus();

                    // Update user status based on subscription status
                    boolean isInGoodStanding = "active".equals(status) || "trialing".equals(status);
                    userService.updateUserAfterPayment(customerId, isInGoodStanding);
                    break;

                case "invoice.paid":
                    // Handle successful payment
                    com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) dataObjectDeserializer.getObject().get();
                    userService.updateUserAfterPayment(invoice.getCustomer(), true);
                    break;

                case "invoice.payment_failed":
                    // Handle failed payment
                    com.stripe.model.Invoice failedInvoice = (com.stripe.model.Invoice) dataObjectDeserializer.getObject().get();
                    userService.updateUserAfterPayment(failedInvoice.getCustomer(), false);
                    break;

                case "customer.subscription.deleted":
                    // Handle subscription cancellation
                    Subscription deletedSubscription = (Subscription) dataObjectDeserializer.getObject().get();
                    userService.updateUserAfterPayment(deletedSubscription.getCustomer(), false);
                    break;

                default:
                    // Ignore unhandled events
                    System.out.println("Unhandled event type: " + eventType);
            }

            return new ResponseEntity<>("Webhook processed", HttpStatus.OK);
        } catch (SignatureVerificationException e) {
            return new ResponseEntity<>("Invalid signature", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Webhook error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}