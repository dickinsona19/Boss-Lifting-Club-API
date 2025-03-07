package com.BossLiftingClub.BossLifting.Stripe;

import com.stripe.exception.InvalidRequestException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.PaymentIntentCreateParams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StripeService {

    public StripeService(@Value("${stripe.secret.key}") String secretKey) {
        Stripe.apiKey = secretKey; // Set the Stripe API key from application.properties or application.yml
    }

    /**
     * Creates a product on Stripe and a price for that product.
     */
    public StripeProductResponse createProduct(String name, String description, long unitAmount, String currency) throws StripeException {
        // Create the product
        ProductCreateParams productParams = ProductCreateParams.builder()
                .setName(name)
                .setDescription(description)
                .build();
        Product product = Product.create(productParams);

        // Create a price for the product
        PriceCreateParams priceParams = PriceCreateParams.builder()
                .setCurrency(currency)
                .setUnitAmount(unitAmount)
                .setProduct(product.getId())
                .build();
        Price price = Price.create(priceParams);

        // Build and return the response
        StripeProductResponse response = new StripeProductResponse();
        response.setProductId(product.getId());
        response.setPriceId(price.getId());
        return response;
    }
    public void createProductAndPrice(String name, long unitAmount, String interval) throws StripeException {
        Product product = Product.create(Map.of("name", name));

        Map<String, Object> priceParams = new HashMap<>();
        priceParams.put("product", product.getId());
        priceParams.put("unit_amount", unitAmount);
        priceParams.put("currency", "usd");

        if (!"one_time".equalsIgnoreCase(interval)) {
            // For recurring prices
            if (!List.of("month", "year", "week", "day").contains(interval.toLowerCase())) {
                throw new IllegalArgumentException("Interval must be one of: month, year, week, day, or one_time");
            }
            priceParams.put("recurring", Map.of("interval", interval.toLowerCase()));
        }
        // If interval is "one_time", omit "recurring" for a one-time price

        Price.create(priceParams);
    }
    public String createCustomer(String email, String fullName, String paymentMethodId) throws StripeException {
        // Create customer with email and name
        Map<String, Object> customerParams = new HashMap<>();
        customerParams.put("email", email);
        customerParams.put("name", fullName); // Set the full name
        Customer customer = Customer.create(customerParams);

        // If paymentMethodId is provided, attach it and set as default
        if (paymentMethodId != null && !paymentMethodId.trim().isEmpty()) {
            // Attach the payment method to the customer
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            paymentMethod.attach(Map.of("customer", customer.getId()));

            // Set as default payment method for invoices
            customer.update(Map.of(
                    "invoice_settings", Map.of("default_payment_method", paymentMethodId)
            ));
        }
        return customer.getId();
    }
    public PaymentIntent createPaymentIntent(String customerId, String priceId) throws StripeException {
        // Retrieve the Price object from Stripe to get the amount and product details
        Price price = Price.retrieve(priceId);
        Long amount = price.getUnitAmount(); // Amount in cents
        String productId = price.getProduct(); // Product ID (e.g., "prod_xxxx")

        // Create the PaymentIntent
        Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        params.put("amount", amount);
        params.put("currency", "usd");
        params.put("payment_method_types", List.of("card"));

        // Add metadata to track the product
        params.put("metadata", Map.of(
                "product_id", productId,
                "price_id", priceId
        ));
        System.out.println(priceId);
        return PaymentIntent.create(params);
    }

    public Subscription createSubscription(String customerId, String priceId) throws StripeException {
        // Step 1: Verify the customer has a default payment method
        Customer customer = Customer.retrieve(customerId);
        String defaultPaymentMethodId = customer.getInvoiceSettings().getDefaultPaymentMethod();
        if (defaultPaymentMethodId == null || defaultPaymentMethodId.isEmpty()) {
            throw new InvalidRequestException(
                    "Customer has no default payment method on file",
                    "default_payment_method",
                    "no_payment_method",
                    null,
                    400,
                    null
            );
        }

        // Step 2: Create the subscription and auto-charge the default payment method
        return Subscription.create(Map.of(
                "customer", customerId,
                "items", List.of(Map.of("price", priceId)),
                "default_payment_method", defaultPaymentMethodId // Explicitly set the default
                // Omit payment_behavior or set to "allow_incomplete" (default is fine here)
        ));
    }
    public void attachPaymentMethod(String customerId, String paymentMethodId) throws StripeException {
        // Retrieve the payment method
        PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);

        // Attach it to the customer (if not already attached)
        Map<String, Object> attachParams = new HashMap<>();
        attachParams.put("customer", customerId);
        paymentMethod.attach(attachParams);

        // Explicitly set as default for invoices
        Customer customer = Customer.retrieve(customerId);
        Map<String, Object> updateParams = new HashMap<>();
        updateParams.put("invoice_settings", Map.of(
                "default_payment_method", paymentMethodId
        ));
        customer.update(updateParams);

        // Verify it’s set (optional debugging)
        Customer updatedCustomer = Customer.retrieve(customerId);
        String defaultPaymentMethod = updatedCustomer.getInvoiceSettings().getDefaultPaymentMethod();
        if (!paymentMethodId.equals(defaultPaymentMethod)) {
            throw new StripeException("Failed to set payment method as default", null, null, null) {
                public String getStripeErrorMessage() { return "Default payment method not set"; }
            };
        }
    }
    public String createFoundingMemberCoupon() throws StripeException {
        Coupon coupon = Coupon.create(Map.of(
                "percent_off", 50.0,
                "duration", "once", // Applies to first month only
                "name", "Founding Member Discount"
        ));
        return coupon.getId();
    }

    public Subscription createFoundingMemberSubscription(String customerId, String priceId, String couponId) throws StripeException {
        return Subscription.create(Map.of(
                "customer", customerId,
                "items", List.of(Map.of("price", priceId)),
                "coupon", couponId
        ));
    }
    /**
     * Creates a PaymentIntent for a one-time payment.
     * The PaymentIntent returns a client secret to use on the client side for confirming payment.
     */
    public PaymentResponse createPaymentIntent(PaymentRequest request) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setDescription(request.getDescription())
                .addPaymentMethodType("card")
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        PaymentResponse response = new PaymentResponse();
        response.setPaymentIntentId(intent.getId());
        response.setClientSecret(intent.getClientSecret());
        return response;
    }

    public PaymentIntent chargeCustomerForProduct(String customerId, String productId) throws StripeException {
        // Step 1: Retrieve the customer and check for a default payment method
        Customer customer = Customer.retrieve(customerId);
        String defaultPaymentMethodId = customer.getInvoiceSettings().getDefaultPaymentMethod();
        if (defaultPaymentMethodId == null || defaultPaymentMethodId.isEmpty()) {
            throw new InvalidRequestException(
                    "Customer has no default payment method on file", // message
                    "default_payment_method",                        // param
                    "no_payment_method",                             // code
                    null,                                            // requestId (null since not from Stripe API)
                    400,                                             // statusCode
                    null                                             // throwable (no cause)
            );
        }

        // Step 2: Fetch a Price associated with the Product (assuming one active price)
        PriceCollection prices = Price.list(Map.of(
                "product", productId,
                "active", true,
                "limit", 1
        ));
        if (prices.getData().isEmpty()) {
            throw new IllegalArgumentException("No active price found for product ID: " + productId);
        }
        Price price = prices.getData().get(0);
        Long amount = price.getUnitAmount();

        // Step 3: Create and confirm the PaymentIntent
        Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        params.put("amount", amount);
        params.put("currency", "usd");
        params.put("payment_method", defaultPaymentMethodId);
        params.put("payment_method_types", List.of("card"));
        params.put("confirm", true);
        params.put("off_session", true);
        params.put("metadata", Map.of(
                "product_id", productId,
                "price_id", price.getId()
        ));

        return PaymentIntent.create(params);
    }
    public String createCheckoutSession(String customerId, String activationProductId, String subscriptionProductId, String successUrl, String cancelUrl) throws StripeException {
        // Fetch Price for Activation Fee (one-time)
        Map<String, Object> activationPriceParams = new HashMap<>();
        activationPriceParams.put("product", activationProductId);
        activationPriceParams.put("active", true);
        // Do not include "recurring" filter for one-time prices
        PriceCollection activationPrices = Price.list(activationPriceParams);
        if (activationPrices.getData().isEmpty()) {
            throw new StripeException("No active one-time price found for activation product ID: " + activationProductId, null, null, null) {

                public String getStripeErrorMessage() { return "No activation price"; }
            };
        }
        // Ensure the price is one-time (not recurring)
        Price activationPrice = activationPrices.getData().stream()
                .filter(price -> price.getRecurring() == null)
                .findFirst()
                .orElseThrow(() -> new StripeException("No one-time price found for activation product ID: " + activationProductId, null, null, null) {
                    public String getStripeErrorMessage() { return "No one-time price"; }
                });
        String activationPriceId = activationPrice.getId();

        // Fetch Price for Subscription (recurring)
        Map<String, Object> subscriptionPriceParams = new HashMap<>();
        subscriptionPriceParams.put("product", subscriptionProductId);
        subscriptionPriceParams.put("active", true);
        subscriptionPriceParams.put("recurring[interval]", "month"); // Adjust interval as needed (e.g., "year")
        PriceCollection subscriptionPrices = Price.list(subscriptionPriceParams);
        if (subscriptionPrices.getData().isEmpty()) {
            throw new StripeException("No active recurring price found for subscription product ID: " + subscriptionProductId, null, null, null) {

                public String getStripeErrorMessage() { return "No subscription price"; }
            };
        }
        String subscriptionPriceId = subscriptionPrices.getData().get(0).getId();

        // Create Checkout session
        Map<String, Object> sessionParams = new HashMap<>();
        sessionParams.put("customer", customerId);
        sessionParams.put("mode", "subscription"); // Subscription mode for recurring
        sessionParams.put("payment_method_types", List.of("card"));
        sessionParams.put("success_url", successUrl);
        sessionParams.put("cancel_url", cancelUrl);

        // Add both line items
        sessionParams.put("line_items", List.of(
                Map.of(
                        "price", activationPriceId, // One-time activation fee
                        "quantity", 1
                ),
                Map.of(
                        "price", subscriptionPriceId, // Recurring subscription
                        "quantity", 1
                )
        ));

        Session session = Session.create(sessionParams);
        return session.getId();
    }

    public String createSetupCheckoutSession(String customerId, String successUrl, String cancelUrl) throws StripeException {
        Map<String, Object> sessionParams = new HashMap<>();
        sessionParams.put("customer", customerId);
        sessionParams.put("mode", "setup"); // Setup mode to collect payment method without charging
        sessionParams.put("payment_method_types", List.of("card"));
        sessionParams.put("success_url", successUrl);
        sessionParams.put("cancel_url", cancelUrl);

        Session session = Session.create(sessionParams);
        return session.getId();
    }

    public String createSetupCheckoutSessionWithMetadata(
            String customerId,
            String successUrl,
            String cancelUrl,
            Map<String, String> metadata
    ) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setCustomer(customerId)
                .setMode(SessionCreateParams.Mode.SETUP) // Setup mode for collecting payment method
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putAllMetadata(metadata) // Add user data to metadata
                .build();

        Session session = Session.create(params);
        return session.getId();
    }

}