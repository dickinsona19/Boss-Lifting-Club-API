package com.BossLiftingClub.BossLifting.User;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

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

    @Transactional
    public boolean updateReferralCode(String currentReferralCode, String newReferralCode) {
        return userRepository.updateReferralCode(currentReferralCode, newReferralCode) > 0;
    }
    @Override
    public User getUserByReferralCode(String referralCode) {
        User userOptional = userRepository.findByReferralCode(referralCode);
        if (userOptional != null) {
            return userOptional;
        }
        return null;
    }

    @Override
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public boolean existsByReferralCode(String referralCode) {
        return userRepository.existsByReferralCode(referralCode);
    }
    @Override
    @Transactional // Not readOnly since we're modifying the entity
    public Optional<User> updateProfilePicture(Long id, byte[] profilePicture) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setProfilePicture(profilePicture);
                    return userRepository.save(user); // Save without re-encoding password
                });
    }
    @Override
    @Transactional(readOnly = true) // readOnly = true since we're only reading
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
    public User updateUserAfterPayment(String stripeCustomerId, boolean standing) {
        Optional<User> optionalUser = userRepository.findByUserStripeMemberId(stripeCustomerId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setIsInGoodStanding(standing); // Payment succeeded
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
    @Override
    @Transactional // Not readOnly since we're modifying the entity
    public User saveWaiverSignature(Long userId, String base64Signature) throws Exception {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Decode Base64 string to byte[]
            byte[] signatureBytes = Base64.getDecoder().decode(base64Signature);
            user.setSignatureData(signatureBytes);

            // Set current date and time
            user.setWaiverSignedDate(LocalDateTime.now());

            return userRepository.save(user);
        } else {
            throw new Exception("User not found with ID: " + userId);
        }
    }
}
