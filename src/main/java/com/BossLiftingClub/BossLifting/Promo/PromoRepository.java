package com.BossLiftingClub.BossLifting.Promo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromoRepository extends JpaRepository<Promo, Long> {
    Optional<Promo> findByCodeToken(String codeToken);
}