package com.BossLiftingClub.BossLifting;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
                .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless API
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // Allow all requests without authentication
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Define allowed origins
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",        // Vite/React default (web dev)
                "http://localhost:8081",        // Local frontend (web dev)
                "https://boss-lifting-club.onrender.com", // Production web 1
                "https://www.cltliftingclub.com", // Production web 2
                "https://joyful-sunflower-8144bb.netlify.app", // Production web 3
                "*"                             // Allow all origins (for mobile app direct requests)
        ));

        // Allowed HTTP methods (same as web)
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS", "DELETE", "PUT"));

        // Allowed headers (same as web)
        config.setAllowedHeaders(List.of("*"));

        // Credentials: Set to true if your app needs cookies or auth headers
        // Note: If true, you CANNOT use "*" in allowedOrigins; list specific origins instead
        config.setAllowCredentials(false); // Change to true if needed

        // Apply this config to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}