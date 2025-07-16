package com.BossLiftingClub.BossLifting.Promo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface PromoRepository extends JpaRepository<Promo, Long> {
    Optional<Promo> findByCodeToken(String codeToken);

    @Query("SELECT p FROM Promo p LEFT JOIN FETCH p.users")
    List<Promo> findAllWithUsers();

    @Query("SELECT p FROM Promo p LEFT JOIN FETCH p.users WHERE p.id = :id")
    Optional<Promo> findByIdWithUsers(Long id);

    @Query("SELECT p FROM Promo p LEFT JOIN FETCH p.users WHERE p.codeToken = :codeToken")
    Optional<Promo> findByCodeTokenWithUsers(String codeToken);


}