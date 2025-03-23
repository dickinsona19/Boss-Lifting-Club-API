package com.BossLiftingClub.BossLifting.User;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
public class UserServiceImpl implements UserService {


    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;

    }
    @Override
    public Optional<User> updateProfilePicture(Long id, byte[] profilePicture) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setProfilePicture(profilePicture);
                    return userRepository.save(user); // Save without re-encoding password
                });
    }
    @Override
    public User signInWithPhoneNumber(String phoneNumber, String password) throws Exception {
        // Find user by phone number
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new Exception("User with phone number " + phoneNumber + " not found"));

        // Validate password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new Exception("Invalid password");
        }

        // Return user if credentials are valid
        return user;
    }
    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    @Override
    public User updateUserAfterPayment(String stripeCustomerId) {
        Optional<User> optionalUser = userRepository.findByUserStripeMemberId(stripeCustomerId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setIsInGoodStanding(true); // Payment succeeded
            return userRepository.save(user);
        } else {
            throw new RuntimeException("User not found for Stripe customer ID: " + stripeCustomerId);
        }
    }
    @Override
    public Optional<User> getUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }
    @Override
    public User getUserByReferralCode(String referralCode){
        return userRepository.findByReferralCode(referralCode);
    }

    @Override
    public Optional<User> deleteUserWithPhoneNumber(String phoneNumber) {
        Optional<User> userOpt = userRepository.findByPhoneNumber(phoneNumber);
        if (userOpt.isPresent()) {
            userRepository.delete(userOpt.get());
            return userOpt; // Return the deleted user
        }
        return Optional.empty(); // No user found
    }

    @Override
    public User updateUserPaymentFailed(String stripeCustomerId) {
        Optional<User> optionalUser = userRepository.findByUserStripeMemberId(stripeCustomerId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setIsInGoodStanding(false); // Payment failed
            return userRepository.save(user);
        } else {
            throw new RuntimeException("User not found for Stripe customer ID: " + stripeCustomerId);
        }
    }
    @Override
    public User save(User user) throws Exception {
        // Encode password
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // Generate unique QR code token
        String token = generateUniqueToken(10);
        while (userRepository.findByEntryQrcodeToken(token).isPresent()) {
            token = generateUniqueToken(10); // Regenerate if not unique
        }
        user.setEntryQrcodeToken(token);
        user.setReferralCode(generateUniqueReferralCode());

        // Save user with token
        return userRepository.save(user);
    }

    private String generateUniqueToken(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private String generateUniqueReferralCode() {
        String code;
        do {
            code = generateUniqueToken(10);
        } while (userRepository.findByReferralCode(code) != null);
        return code;
    }

    @Override
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> getUserByBarcodeToken(String barcodeToken){
       return userRepository.findByEntryQrcodeToken(barcodeToken);
    }
    @Override
    public User signIn(Map<String, String> requestBody) throws Exception {
        String phoneNumber = requestBody.get("phoneNumber");
        String password = requestBody.get("password");

        // Basic validation
        if (phoneNumber == null || password == null) {
            throw new Exception("Phone number and password are required");
        }

        Optional<User> optionalUser = userRepository.findByPhoneNumber(phoneNumber);

        if (optionalUser.isEmpty()) {
            throw new Exception("Invalid phone number or password");
        }

        User user = optionalUser.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new Exception("Invalid phone number or password");
        }

        return user; // Return the user object on successful login
    }
}
