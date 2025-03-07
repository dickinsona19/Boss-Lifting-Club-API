package com.BossLiftingClub.BossLifting.Stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubscriptionController {

    @GetMapping("/subscribe")
    public PaymentResponse subscribe() throws StripeException {
        // Use the provided recurring Price ID
        String subscriptionPriceId = "price_1QvR4oRs8b43cBltIs6X4Byk";

        // Build the Checkout Session parameters for a subscription
        SessionCreateParams params = SessionCreateParams.builder()
                .setSuccessUrl("https://yourdomain.com/success") // URL to redirect upon successful checkout
                .setCancelUrl("https://yourdomain.com/cancel")   // URL to redirect if checkout is canceled
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPrice(subscriptionPriceId)
                                .build()
                )
                // Set mode to SUBSCRIPTION to indicate a recurring payment
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .build();

        // Create the Checkout Session
        Session session = Session.create(params);

        // Return the session URL in your response model so that the client can redirect the customer
        PaymentResponse response = new PaymentResponse();
        response.setPaymentLink(session.getUrl());
        return response;
    }
}
