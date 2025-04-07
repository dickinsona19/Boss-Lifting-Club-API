package com.BossLiftingClub.BossLifting.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserService {
    List<User> findAll();
    Optional<User> findById(Long id);
    User save(User user) throws Exception;
    void deleteById(Long id);
    Optional<User> getUserByBarcodeToken(String barcodeToken);
    User updateUserAfterPayment(String stripeCustomerId);
    User updateUserPaymentFailed(String stripeCustomerId);
    Optional<User> getUserByPhoneNumber(String phoneNumber);
    Optional<User> deleteUserWithPhoneNumber(String PhoneNumber);
    User signIn(Map<String, String> requestBody) throws Exception;
    User signInWithPhoneNumber(String phoneNumber, String password) throws Exception;
    Optional<User> updateProfilePicture(Long id, byte[] profilePicture);
    User getUserByReferralCode(String referralCode);
    User saveWaiverSignature(Long userId, String base64Signature) throws Exception;
    User updateUser(User user);
    boolean existsByReferralCode(String referralCode);
}
