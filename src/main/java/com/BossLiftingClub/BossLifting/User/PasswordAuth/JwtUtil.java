package com.BossLiftingClub.BossLifting.User.PasswordAuth;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {
    private String secret = "X7kP9mN2vQ8rT5wY1zA3bC6dE9fG2hJ4kL5mN6pQ8rS9tU0vW1xY2zA3B4C5D6E7F8G9H0I1J2K3L4M5N6O7P8Q9"; // Replace with a strong secret (store in env vars in production)
    private long expiration = 1000 * 60 * 60 * 10; // 10 hours in milliseconds

    // Generate token (used during login, included for context)
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    // Validate token
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true; // Token is valid if no exception is thrown
        } catch (Exception e) {
            return false; // Invalid token (expired, tampered, etc.)
        }
    }

    // Extract username from token
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}