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
}