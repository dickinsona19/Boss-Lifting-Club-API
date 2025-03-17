package com.BossLiftingClub.BossLifting.User.PasswordAuth;


import com.BossLiftingClub.BossLifting.User.User;
import com.BossLiftingClub.BossLifting.User.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class PasswordAuthController {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserService userService;

    @GetMapping("/signin/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("error", "Invalid or missing token");
            return ResponseEntity.status(401).body(response);
        }

        String token = authHeader.substring(7);
        if (jwtUtil.validateToken(token)) {
            String phoneNumber = jwtUtil.getUsernameFromToken(token); // Phone number is the subject
            Optional<User> user = userService.getUserByPhoneNumber(phoneNumber); // Fetch full user

            if (user != null) {
                response.put("message", "Token is valid");
                response.put("user", user); // Return full User object
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "User not found for valid token");
                return ResponseEntity.status(404).body(response);
            }
        } else {
            response.put("error", "Token is invalid or expired");
            return ResponseEntity.status(401).body(response);
        }
    }
}