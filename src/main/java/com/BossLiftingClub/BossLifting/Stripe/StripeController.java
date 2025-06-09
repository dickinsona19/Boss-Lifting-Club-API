package com.BossLiftingClub.BossLifting.Stripe;

import com.BossLiftingClub.BossLifting.Stripe.ProcessedEvent.EventService;
import com.BossLiftingClub.BossLifting.Stripe.RequestsAndResponses.*;
import com.BossLiftingClub.BossLifting.Stripe.Transfers.TransferService;
import com.BossLiftingClub.BossLifting.User.*;
import com.BossLiftingClub.BossLifting.User.Membership.Membership;
import com.BossLiftingClub.BossLifting.User.Membership.MembershipRepository;
import com.BossLiftingClub.BossLifting.User.UserTitles.UserTitles;
import com.BossLiftingClub.BossLifting.User.UserTitles.UserTitlesRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private final EventService eventService;
    private final TransferService transferService;
    @Autowired
    private JavaMailSender mailSender;
    public StripeController(EventService eventService, TransferService transferService, UserService userService, StripeService stripeService, @Value("${stripe.webhook.secret}") String webhookSecret, @Value("${stripe.webhook.subscriptionSecret}") String webhookSubscriptionSecret, UserTitlesRepository userTitlesRepository, MembershipRepository membershipRepository, UserRepository userRepository) {
        this.eventService = eventService;
        this.stripeService = stripeService;
        this.webhookSecret = webhookSecret;
        this.userService = userService;
        this.userTitlesRepository = userTitlesRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.webhookSubscriptionSecret = webhookSubscriptionSecret;
        this.transferService = transferService;
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
                        createSubscriptions(customerId, paymentMethodId, user.isLockedInRate());

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
        user.setLockedInRate(metadata.get("lockedInRate"));

        if (metadata.get("referredUserId") != null) {
            User referrer = userRepository.findById(Long.valueOf(metadata.get("referredUserId")))
                    .orElseThrow(() -> new RuntimeException("Referred User not found in database"));
            user.setReferredBy(referrer);

            // Create and apply discount coupon for referrer
            try {
                String referrerStripeId = referrer.getUserStripeMemberId();
                if (referrerStripeId != null) {
                    // Retrieve the referrer's subscription
                    SubscriptionListParams subscriptionParams = SubscriptionListParams.builder()
                            .setCustomer(referrerStripeId)
                            .build();
                    SubscriptionCollection subscriptions = Subscription.list(subscriptionParams);

                    if (!subscriptions.getData().isEmpty()) {
                        Subscription referrerSubscription = subscriptions.getData().get(0);

                        // Check if subscription already has a coupon
                        if (referrerSubscription.getDiscount() != null && referrerSubscription.getDiscount().getCoupon() != null) {
                            // Extend existing coupon duration by 1 month
                            Coupon existingCoupon = referrerSubscription.getDiscount().getCoupon();
                            Long newDurationInMonths = 1L; // Default to 1 month if no duration specified
                            if (existingCoupon.getDurationInMonths() != null) {
                                newDurationInMonths = existingCoupon.getDurationInMonths() + 1;
                            }

                            // Create a new coupon with extended duration
                            CouponCreateParams couponParams = CouponCreateParams.builder()
                                    .setPercentOff(BigDecimal.valueOf(100.0))
                                    .setDuration(CouponCreateParams.Duration.REPEATING)
                                    .setDurationInMonths(newDurationInMonths)
                                    .setName("Extended Referral Discount")
                                    .build();
                            Coupon newCoupon = Coupon.create(couponParams);

                            // Update subscription with new coupon
                            SubscriptionUpdateParams updateParams = SubscriptionUpdateParams.builder()
                                    .setCoupon(newCoupon.getId())
                                    .build();
                            Subscription updatedSubscription = referrerSubscription.update(updateParams);
                            System.out.println("Extended coupon applied to referrer's subscription: " + updatedSubscription.getId());
                        } else {
                            // Create a new 100% off coupon for 1 month
                            CouponCreateParams couponParams = CouponCreateParams.builder()
                                    .setPercentOff(BigDecimal.valueOf(100.0))
                                    .setDuration(CouponCreateParams.Duration.REPEATING)
                                    .setDurationInMonths(1L)
                                    .setName("Referral Discount")
                                    .build();
                            Coupon coupon = Coupon.create(couponParams);

                            // Apply the coupon to the referrer's subscription
                            SubscriptionUpdateParams updateParams = SubscriptionUpdateParams.builder()
                                    .setCoupon(coupon.getId())
                                    .build();
                            Subscription updatedSubscription = referrerSubscription.update(updateParams);
                            System.out.println("Coupon applied to referrer's subscription: " + updatedSubscription.getId());
                        }
                    }
                }
            } catch (StripeException e) {
                System.err.println("Error applying referral coupon: " + e.getMessage());
                // Log the error but don't fail the user creation
            }
        }

        user.setIsInGoodStanding(false); // Still false until payment succeeds later
        UserTitles foundingUserTitle = userTitlesRepository.findByTitle(metadata.get("userTitle"))
                .orElseThrow(() -> new RuntimeException("Founding user title not found in database"));
        Membership membership = membershipRepository.findByName(metadata.get("membership"))
                .orElseThrow(() -> new RuntimeException("Membership not found in database"));
        user.setMembership(membership);
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
            metadata.put("lockedInRate",userRequest.getLockedInRate());
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

            String eventId = event.getId();
            String eventType = event.getType();

            // Check if event was already processed
            if (eventService.isEventProcessed(eventId)) {
                System.out.println("Event " + eventId + " already processed, skipping.");
                return new ResponseEntity<>("Event already processed", HttpStatus.OK);
            }

            // Handle invoice.paid (transfer 4% fee to Connected Account)
//            if ("invoice.paid".equals(eventType)) {
//                Invoice invoice = (Invoice) dataObjectDeserializer.getObject().orElse(null);
//                if (invoice != null && invoice.getSubscription() != null && invoice.getCharge() != null) {                    String chargeId = invoice.getCharge();
//                    // Check if a transfer already exists for this charge
//                    if (transferService.hasProcessedCharge(chargeId)) {
//                        System.out.println("Transfer already exists for charge " + chargeId + ", skipping.");
//                        return new ResponseEntity<>("Webhook processed", HttpStatus.OK);
//                    }
//
//                    Charge charge = Charge.retrieve(chargeId);
//                    if (charge != null && "succeeded".equals(charge.getStatus())) {
//                        Subscription subscription = Subscription.retrieve(invoice.getSubscription());
//                        long feeCents = calculateFeeCents(subscription);
//                        if (feeCents > 0) {
//                            try {
//                                // Check available balance before transfer
//                                Balance balance = Balance.retrieve();
//                                long availableBalance = balance.getAvailable().stream()
//                                        .filter(b -> "usd".equals(b.getCurrency()))
//                                        .mapToLong(Balance.Available::getAmount)
//                                        .sum();
//                                if (availableBalance < feeCents) {
//                                    System.err.println("Insufficient balance: " + (availableBalance / 100.0) + " USD, needed: " + (feeCents / 100.0));
//                                    return new ResponseEntity<>("Insufficient balance", HttpStatus.INTERNAL_SERVER_ERROR);
//                                }
//
//                                TransferCreateParams transferParams = TransferCreateParams.builder()
//                                        .setAmount(feeCents)
//                                        .setCurrency("usd")
//                                        .setDestination("acct_1RDvRj4gikNsBARu")
//                                        .setSourceTransaction(chargeId)
//                                        .build();
//                                Transfer transfer = Transfer.create(transferParams);
//                                System.out.println("Transferred " + (feeCents / 100.0) + " to Connected Account for invoice " + invoice.getId());
//                                // Store transfer record
//                                transferService.saveTransfer(chargeId, invoice.getId(), transfer.getId());
//                            } catch (StripeException e) {
//                                System.err.println("Transfer failed for invoice " + invoice.getId() + ": " + e.getMessage());
//                                return new ResponseEntity<>("Transfer error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//                            }
//                        }
//                        // Update user status
//                        userService.updateUserAfterPayment(invoice.getCustomer(), true);
//                    } else {
//                        System.out.println("Charge was not successful. Skipping fee transfer for invoice " + invoice.getId());
//                    }
//                }
//            }


            // Handle other event types
            switch (eventType) {
                case "customer.subscription.created":
                case "customer.subscription.updated":
                    Subscription subscription = (Subscription) dataObjectDeserializer.getObject().get();
                    String customerId = subscription.getCustomer();
                    String status = subscription.getStatus();
                    boolean isInGoodStanding = "active".equals(status) || "trialing".equals(status);
                    userService.updateUserAfterPayment(customerId, isInGoodStanding);
                    break;

                case "invoice.payment_failed":
                    Invoice failedInvoice = (Invoice) dataObjectDeserializer.getObject().get();
                    if (failedInvoice != null && failedInvoice.getSubscription() != null) {
                        userService.updateUserAfterPayment(failedInvoice.getCustomer(), false);
                    }
                    break;

                case "customer.subscription.deleted":
                    Subscription deletedSubscription = (Subscription) dataObjectDeserializer.getObject().get();
                    userService.updateUserAfterPayment(deletedSubscription.getCustomer(), false);
                    break;

                default:
                    System.out.println("Unhandled event type: " + eventType + ", ID: " + eventId);
            }

            // Mark event as processed
            eventService.markEventProcessed(eventId);
            return new ResponseEntity<>("Webhook processed", HttpStatus.OK);
        } catch (SignatureVerificationException e) {
            return new ResponseEntity<>("Invalid signature", HttpStatus.BAD_REQUEST);
        } catch (StripeException e) {
            System.err.println("Stripe API error: " + e.getMessage());
            return new ResponseEntity<>("Stripe error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            return new ResponseEntity<>("Webhook error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    private void createSubscriptions(String customerId, String paymentMethodId, String membershipPrice) throws StripeException {
        // Validate customer and payment method
        Customer customer = Customer.retrieve(customerId);
        if (customer.getDeleted() != null && customer.getDeleted()) {
            throw new RuntimeException("Customer is deleted: " + customerId);
        }
        if (paymentMethodId == null || !PaymentMethod.retrieve(paymentMethodId).getCustomer().equals(customerId)) {
            throw new RuntimeException("Invalid payment method for customer: " + customerId);
        }
        System.out.println("Customer " + customerId + " has valid payment method: " + paymentMethodId);

        // Determine billing cycle based on current date
        LocalDate currentDate = LocalDate.now(ZoneId.of("UTC"));
        System.out.println("Setting main subscription billing cycle to current date: " + currentDate);

        // Map membershipPrice to Price IDs
        String mainPriceId;
        String mainFeePriceId;
        switch (membershipPrice) {
            case "89.99":
                mainPriceId = "price_1R6aIfGHcVHSTvgIlwN3wmyD";
                mainFeePriceId = "price_1RF4FpGHcVHSTvgIKM8Jilwl"; // $3.60
                break;
            case "99.99":
                mainPriceId = "price_1RF313GHcVHSTvgI4HXgjwOA";
                mainFeePriceId = "price_1RF4GlGHcVHSTvgIVojlVrn7"; // $4.00
                break;
            case "109.99":
                mainPriceId = "price_1RF31hGHcVHSTvgIbTnGo4vT";
                mainFeePriceId = "price_1RF4IsGHcVHSTvgIYOoYfxb9"; // $4.40
                break;
            case "948.00":
                mainPriceId = "price_1RJJuTGHcVHSTvgI2pVN6hfx";
                break;
            default:
                throw new IllegalArgumentException("Invalid membership price: " + membershipPrice);
        }

        // Application fee Price ID (one-time)
        String applicationFeePriceId = "price_1RJOFhGHcVHSTvgI08VPh4XY"; // One-time application fee Price ID

        // Step 1: Create a one-time InvoiceItem for the application fee
        InvoiceItemCreateParams invoiceItemParams = InvoiceItemCreateParams.builder()
                .setCustomer(customerId)
                .setPrice(applicationFeePriceId) // One-time $50 application fee
                .setQuantity(1L)
                .build();

        InvoiceItem invoiceItem = InvoiceItem.create(invoiceItemParams);

        // Step 2: Create a transfer for the application fee to the destination account
        // Since InvoiceItems don't support direct transfers, we'll need to handle this after the payment
        // We'll finalize the invoice immediately to charge the customer
        com.stripe.model.Invoice invoice = com.stripe.model.Invoice.create(
                com.stripe.param.InvoiceCreateParams.builder()
                        .setCustomer(customerId)
                        .setCollectionMethod(com.stripe.param.InvoiceCreateParams.CollectionMethod.CHARGE_AUTOMATICALLY)
                        .build()
        );
        invoice.finalizeInvoice();




        // Step 4: Create the subscription (without the application fee)
        SubscriptionCreateParams.Builder subscriptionBuilder = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice(mainPriceId)
                        .build())
                .setDefaultPaymentMethod(paymentMethodId)
                .setTransferData(SubscriptionCreateParams.TransferData.builder()
                        .setDestination("acct_1RDvRj4gikNsBARu")
                        .setAmountPercent(new BigDecimal("4.0"))
                        .build());

// Check if the membership price is not 948 before adding the tax rate
        if (!membershipPrice.equals("948.00")) {
            subscriptionBuilder.addDefaultTaxRate("txr_1RIsQGGHcVHSTvgIF3A1Nacp");
        }
        SubscriptionCreateParams subscriptionParams = subscriptionBuilder.build();
        Subscription subscription = Subscription.create(subscriptionParams);

        System.out.println("Subscription created: " + subscription.getId());



        // Maintenance subscription logic
        LocalDate currentYearJanuaryBilling = LocalDate.of(currentDate.getYear(), 1, 1);
        LocalDate currentYearJulyBilling = LocalDate.of(currentDate.getYear(), 7, 1);
        boolean isBeforeJuly = currentDate.isBefore(currentYearJulyBilling) || currentDate.isEqual(currentYearJulyBilling);
        LocalDate previousBillingDate = isBeforeJuly ? currentYearJanuaryBilling : currentYearJulyBilling;
        long nextBillingMonth = isBeforeJuly ? 7L : 1L; // July 1 or January 1
        long nextBillingDay = 1L;
        int nextBillingYear = isBeforeJuly ? currentDate.getYear() : currentDate.getYear() + 1;

        // Calculate trial end timestamp for next billing date
        LocalDate nextBillingDate = LocalDate.of(nextBillingYear, (int) nextBillingMonth, (int) nextBillingDay);
        ZonedDateTime nextBillingDateTime = nextBillingDate.atStartOfDay(ZoneId.of("UTC"));
        long trialEndTimestamp = nextBillingDateTime.toEpochSecond();

        // Validate trial end is in the future
        long currentTimestamp = Instant.now().getEpochSecond();
        if (trialEndTimestamp <= currentTimestamp) {
            throw new IllegalArgumentException("Trial end timestamp must be in the future: " + nextBillingDate);
        }

        // Create maintenance subscription with fee as a recurring item
        SubscriptionCreateParams.Builder maintenanceParamsBuilder = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice("price_1RF30SGHcVHSTvgIpegCzQ0m") // $59.99
                        .build())
                .setDefaultPaymentMethod(paymentMethodId)
                .setProrationBehavior(SubscriptionCreateParams.ProrationBehavior.NONE)
                .addExpand("schedule")
                .setTrialEnd(trialEndTimestamp)
                .setTransferData(SubscriptionCreateParams.TransferData.builder()
                        .setDestination("acct_1RDvRj4gikNsBARu")
                        .setAmountPercent(new BigDecimal("4.0"))
                        .build());

        System.out.println("Setting maintenance subscription with trial until " + nextBillingDate +
                ", charging full $67.26 for period starting " + previousBillingDate);

        try {
            Subscription maintenanceSubscription = Subscription.create(maintenanceParamsBuilder.build());
            System.out.println("Maintenance subscription created with ID: " + maintenanceSubscription.getId());
        } catch (StripeException e) {
            System.err.println("Failed to create maintenance subscription: " + e.getMessage() + "; request-id: " + e.getRequestId());
            throw e;
        }
    }
    @PostMapping("/update-subscription-transfer")
    public String updateSubscriptionTransferData(@RequestParam String subId) {
        try {
            // Step 1: Retrieve the subscription
            Subscription subscription = Subscription.retrieve(subId);

            // Step 2: Update the subscription with new transfer_data
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> transferData = new HashMap<>();
            transferData.put("destination", "acct_1RDvRj4gikNsBARu");
            transferData.put("amount_percent", 4);
            params.put("transfer_data", transferData);

            Subscription updatedSubscription = subscription.update(params);

            return "Subscription transfer data updated successfully. Subscription ID: " + updatedSubscription.getId();

        } catch (StripeException e) {
            return "Error: " + e.getMessage();
        }
    }



    @PostMapping("/{userId}/sendPasswordEmail")
    public ResponseEntity<String> sendPasswordResetEmail(@PathVariable Integer userId, @RequestParam String cusId) {
        // Validate cusId format
        if (cusId == null || !cusId.startsWith("cus_")) {
            return ResponseEntity.badRequest().body("Invalid customer ID format");
        }

        try {


            // Fetch customer email from Stripe
            Customer customer = Customer.retrieve(cusId);
            String toEmail = customer.getEmail();
            if (toEmail == null || toEmail.isEmpty()) {
                return ResponseEntity.badRequest().body("No email found for customer ID: " + cusId);
            }

            // Prepare and send email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            // Hardcoded password reset link with userId
            String resetLink = "https://www.cltliftingclub.com/reset-password?id=" + userId;

            // Professional email content for Clt Lifting
            String subject = "Password Reset Request for Clt Lifting";
            String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2>Password Reset Request</h2>
                    <p>Dear Member,</p>
                    <p>We received a request to reset your password for your CLT Lifting Club LLC account. Click the link below to reset your password:</p>
                    <p><a href="%s" style="background-color: #28a745; color: #fff; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Reset Your Password</a></p>
                    <p>If you did not request a password reset, please ignore this email or contact our support team at support@cltlifting.com.</p>
                    <p>Thank you for choosing CLT Lifting Club LLC!</p>
                    <p>Best regards,<br>The CLT Lifting Team</p>
                    <hr>
                    <p style="font-size: 12px; color: #777;">CLT Lifting Club LLC, 3100 South Blvd, Charlotte, NC, USA</p>
                </body>
                </html>
                """.formatted(resetLink);

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("cltliftingclubtech@gmail.com");

            mailSender.send(message);

            return ResponseEntity.ok("Password reset email sent to " + toEmail);
        } catch (StripeException e) {
            return ResponseEntity.status(400).body("Stripe error: " + e.getMessage());
        } catch (MessagingException e) {
            return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
        }
    }
    @PostMapping("/{userId}/sendAndroidEmail")
    public ResponseEntity<String> sendTestEmail(@PathVariable Long userId) {
        // Fetch user from database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Validate stripeCusId format
        String stripeCusId = user.getUserStripeMemberId();
        if (stripeCusId == null || !stripeCusId.startsWith("cus_")) {
            return ResponseEntity.badRequest().body("Invalid customer ID format");
        }

        // Fetch customer email from Stripe
        String toEmail;
        try {
            Customer customer = Customer.retrieve(stripeCusId);
            toEmail = customer.getEmail();
            if (toEmail == null || toEmail.isEmpty()) {
                return ResponseEntity.badRequest().body("No email found for customer ID: " + stripeCusId);
            }
        } catch (StripeException e) {
            return ResponseEntity.status(400).body("Stripe error: " + e.getMessage());
        }

        // Prepare and send email
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            // Testing link and feedback form link (replace with actual URLs)
            String testingLink = "https://play.google.com/apps/test/com.adickinson.CltLiftingClub/1";

            String contact = "support@cltliftingclub.com";

            // Professional HTML email content for CLT Lifting Club
            String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2>Test the CLT Lifting Club App</h2>
                    <p>Dear Member,</p>
                    <p>You’re invited to test the CLT Lifting Club app, launching on the Play Store soon!</p>
                    <p><a href="%s" style="background-color: #28a745; color: #fff; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Join the Test Now</a></p>
                    <p>Steps:<br>
                    1. Click the link above.<br>
                    2. Install the app from the Play Store (no “Unknown Sources” needed).<br>
                    3. Log in with your membership credentials (contact %s if unsure).<br>
                    4. Requires Android 8.0+.<br>
                    <p>Thank you for choosing CLT Lifting Club LLC!</p>
                    <p>Best regards,<br>The CLT Lifting Team</p>
                    <hr>
                    <p style="font-size: 12px; color: #777;">CLT Lifting Club LLC, 3100 South Blvd, Charlotte, NC, USA</p>
                </body>
                </html>
                """.formatted(testingLink, contact);

            helper.setTo(toEmail);
            helper.setSubject("Test the CLT Lifting Club App for Play Store Launch – Join Now!");
            helper.setText(htmlContent, true);
            helper.setFrom("cltliftingclubtech@gmail.com");

            mailSender.send(message);

            return ResponseEntity.ok("Test email sent to " + toEmail);
        } catch (MessagingException e) {
            return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
        }
    }
    @PostMapping("/update-payment-method")
    public Map<String, Object> updatePaymentMethod(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String customerId = request.get("customerId");
            String paymentMethodId = request.get("paymentMethodId");

            // Attach payment method to customer
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            paymentMethod.attach(new HashMap<String, Object>() {{
                put("customer", customerId);
            }});

            // Set as default payment method
            Customer customer = Customer.retrieve(customerId);
            customer.update(new HashMap<String, Object>() {{
                put("invoice_settings", new HashMap<String, Object>() {{
                    put("default_payment_method", paymentMethodId);
                }});
            }});

            response.put("success", true);
        } catch (StripeException e) {
            response.put("error", e.getMessage());
        }
        return response;
    }
    @PostMapping("/create-setup-intent")
    public Map<String, String> createSetupIntent(@RequestBody Map<String, String> request) throws StripeException {
        String customerId = request.get("customerId");
        SetupIntent setupIntent = SetupIntent.create(
                new HashMap<String, Object>() {{
                    put("customer", customerId);
                    put("payment_method_types", new String[]{"card"});
                }}
        );
        return new HashMap<String, String>() {{
            put("clientSecret", setupIntent.getClientSecret());
        }};
    }

}