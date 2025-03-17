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
                        .requestMatchers("/api/auth/**", "/auth/**", "/users/**", "/users/password/**","/users/**/picture").permitAll() // Allow your endpoints
                        .anyRequest().authenticated() // Secure everything else
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",        // Vite/React default
                "http://localhost:8081",        // Your current frontend
                "https://boss-lifting-club.onrender.com", //Production 1
                "www.cltliftingclub.com"// Production 2
        ));
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS", "DELETE", "PUT")); // Include OPTIONS for preflight
        config.setAllowedHeaders(List.of("*")); // Allow all headers
        config.setAllowCredentials(false); // No cookies/credentials needed yet

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // Apply to all paths
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}