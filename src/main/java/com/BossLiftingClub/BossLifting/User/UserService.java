package com.BossLiftingClub.BossLifting.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> findAll();
    Optional<User> findById(Long id);
    User save(User user);
    void deleteById(Long id);
    Optional<User> getUserByBarcodeToken(String barcodeToken);
}
