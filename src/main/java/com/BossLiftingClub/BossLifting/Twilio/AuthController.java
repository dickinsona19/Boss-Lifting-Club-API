package com.BossLiftingClub.BossLifting.Twilio;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final TwilioService twilioService;
    private  String waiverBaseUrl =" https://boss-lifting-club-api-1.onrender.com/signWaiver";
    public AuthController(TwilioService twilioService) {
        this.twilioService = twilioService;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOTP(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");
        try {
            String sid = twilioService.sendOTP(phoneNumber);
            return ResponseEntity.ok(Map.of("message", "OTP sent", "sid", sid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to send OTP: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Boolean>> verifyOTP(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");
        String code = request.get("otp");
        boolean isValid = twilioService.verifyOTP(phoneNumber, code);
        return ResponseEntity.ok(Map.of("isValid", isValid));
    }

    @PostMapping("/send-waiver-sms")
    public ResponseEntity<Map<String, String>> sendWaiverSMS(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");
        String userId = request.get("userId"); // Optional: include userId in request
        String isPotentialUser = request.getOrDefault("isPotentialUser", "false"); // Default to true

        try {
            // Validate phone number format (E.164, e.g., +1234567890)
            if (!phoneNumber.matches("^\\+?[1-9]\\d{1,14}$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid phone number format"));
            }

            // Construct waiver URL
            String waiverUrl = String.format("%s?userId=%s&isPotentialUser=%s", waiverBaseUrl, userId, isPotentialUser);

            // Message content
            String message = "Sign this waiver for a free day pass at CLT Lifting Club: " + waiverUrl;

            // Send SMS via TwilioService
            String sid = twilioService.sendSMS("+1"+phoneNumber, message);

            return ResponseEntity.ok(Map.of("message", "Waiver SMS sent", "sid", sid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to send waiver SMS: " + e.getMessage()));
        }
    }
}