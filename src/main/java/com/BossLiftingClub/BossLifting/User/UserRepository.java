package com.BossLiftingClub.BossLifting.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEntryQrcodeToken(String token);
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByUserStripeMemberId(String stripeCustomerId);
    User findByReferralCode(String referralCode);
    boolean existsByReferralCode(String referralCode);
}
