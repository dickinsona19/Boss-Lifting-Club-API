package com.BossLiftingClub.BossLifting.Promo;

import java.util.List;
import java.util.Optional;

public interface PromoService {

    List<Promo> findAll();

    Optional<Promo> findById(Long id);

    Optional<Promo> findByCodeToken(String codeToken);

    Promo save(Promo promo);

    void deleteById(Long id);

    void addUserToPromo(String codeToken, Long userId);
}