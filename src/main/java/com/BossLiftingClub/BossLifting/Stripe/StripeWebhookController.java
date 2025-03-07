//package com.BossLiftingClub.BossLifting.Stripe;
//
//import com.stripe.exception.SignatureVerificationException;
//import com.stripe.model.Event;
//import com.stripe.net.Webhook;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestHeader;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//public class StripeWebhookController {
//    @PostMapping("/webhook")
//    public ResponseEntity<String> handleWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) throws SignatureVerificationException {
//        String endpointSecret = "your_webhook_secret";
//        Event event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
//
//        switch (event.getType()) {
//            case "payment_intent.succeeded":
//                // Handle successful payment
//                break;
//            case "customer.subscription.updated":
//                // Handle subscription changes
//                break;
//        }
//        return ResponseEntity.ok("Received");
//    }
//}