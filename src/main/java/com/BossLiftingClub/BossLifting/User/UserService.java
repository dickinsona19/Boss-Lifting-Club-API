package com.BossLiftingClub.BossLifting.User;

import java.util.List;
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
}
