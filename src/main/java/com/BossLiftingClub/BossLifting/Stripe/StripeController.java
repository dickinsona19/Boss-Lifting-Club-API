package com.BossLiftingClub.BossLifting.Stripe;

import com.BossLiftingClub.BossLifting.Stripe.RequestsAndResponses.*;
import com.BossLiftingClub.BossLifting.User.*;
import com.BossLiftingClub.BossLifting.User.Membership.Membership;
import com.BossLiftingClub.BossLifting.User.Membership.MembershipRepository;
import com.BossLiftingClub.BossLifting.User.UserTitles.UserTitles;
import com.BossLiftingClub.BossLifting.User.UserTitles.UserTitlesRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/test-product")
    public StripeProductResponse testProduct() throws StripeException {
        return stripeService.createProduct("Test Product", "A test item", 1000L, "usd");

    }

    @GetMapping("/test-payment")
    public PaymentResponse testPayment() throws StripeException {
        // Build the Checkout Session parameters
        SessionCreateParams params = SessionCreateParams.builder()
                .setSuccessUrl("https://localhost:8081/success")
                .setCancelUrl("https://localhost:8081/cancel")
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(1000L) // amount in cents ($10.00)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Test Payment")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .build();

        // Create the Checkout Session
        Session session = Session.create(params);

        // Build and return the PaymentResponse with the payment link
        PaymentResponse response = new PaymentResponse();
        response.setPaymentLink(session.getUrl());
        return response;
    }

    @GetMapping("/test-payment/{token}")
    public PaymentResponse testPaymentWithID(@PathVariable String token) throws StripeException {
        // Replace "price_XXXXXXXXXXXX" with your actual Price ID from the Stripe Dashboard.
        String priceId = "price_"+token;

        // Build the Checkout Session parameters using the existing Price ID.
        SessionCreateParams params = SessionCreateParams.builder()
                .setSuccessUrl("https://yourdomain.com/success")
                .setCancelUrl("https://yourdomain.com/cancel")
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPrice(priceId)
                                .build()
                )
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .build();

        // Create the Checkout Session
        Session session = Session.create(params);

        // Build and return the PaymentResponse with the payment link
        PaymentResponse response = new PaymentResponse();
        response.setPaymentLink(session.getUrl());
        return response;
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
                        user.setUserTitles(foundingUserTitle);
                        user.setUserStripeMemberId(customerId);
                        System.out.println("userLog: " + user);
                        System.out.println("getReferredMembersDto: " + user.getReferredMembersDto());
                        userService.save(user);

                        System.out.println("User created with ID: " + user.getId() + " for customer: " + customerId);

                        // Create a subscription starting April 12, 2025
                        long trialEndTimestamp = 1744416000L; // UNIX timestamp for April 12, 2025, 00:00 UTC
                        String priceId = "price_12345"; // Replace with your actual $79.99/month Price ID from Stripe

                        SubscriptionCreateParams subscriptionParams = SubscriptionCreateParams.builder()
                                .setCustomer(customerId)
                                .addItem(SubscriptionCreateParams.Item.builder()
                                        .setPrice(priceId)
                                        .build())
                                .setDefaultPaymentMethod(paymentMethodId)
                                .setTrialEnd(trialEndTimestamp)
                                .build();

                        Subscription subscription = Subscription.create(subscriptionParams);
                        System.out.println("Subscription created with ID: " + subscription.getId() + " starting on April 12, 2025");

                    } else {
                        // No payment method provided, delete the Stripe customer
                        System.out.println("No payment method attached in setup intent: " + setupIntentId);
                        stripeService.deleteCustomer(customerId);
                        System.out.println("Deleted Stripe customer " + customerId + " due to no payment method.");
                        return ResponseEntity.status(400).body("No payment method found in setup intent");
                    }

                } else if ("subscription".equals(mode)) {
                    // Handle subscription mode (unchanged)
                    String subscriptionId = session.getSubscription();
                    if (subscriptionId == null) {
                        return ResponseEntity.status(400).body("No subscription found in session");
                    }

                    Subscription subscription = Subscription.retrieve(subscriptionId);
                    String invoiceId = subscription.getLatestInvoice();
                    if (invoiceId == null) {
                        return ResponseEntity.status(400).body("No invoice found for subscription");
                    }
                    Invoice invoice = Invoice.retrieve(invoiceId);
                    String paymentIntentId = invoice.getPaymentIntent();

                    if (paymentIntentId != null) {
                        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
                        String paymentMethodId = paymentIntent.getPaymentMethod();
                        stripeService.attachPaymentMethod(customerId, paymentMethodId);
                    } else {
                        String defaultPaymentMethodId = subscription.getDefaultPaymentMethod();
                        if (defaultPaymentMethodId != null) {
                            stripeService.attachPaymentMethod(customerId, defaultPaymentMethodId);
                        } else {
                            System.out.println("No payment method found for subscription: " + subscriptionId);
                        }
                    }

                    userService.updateUserAfterPayment(customerId);
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
}