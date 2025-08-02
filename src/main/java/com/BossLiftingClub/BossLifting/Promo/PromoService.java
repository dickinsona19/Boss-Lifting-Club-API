package com.BossLiftingClub.BossLifting.Promo;

import java.util.List;
import java.util.Optional;

public interface PromoService {

    List<PromoDTO> findAll();

    Optional<PromoDTO> findById(Long id);

    Optional<PromoDTO> findByCodeToken(String codeToken);

    Promo save(Promo promo);

    void deleteById(Long id);

    void addUserToPromo(String codeToken, Long userId);
    Optional<PromoDTO> incrementUrlVisitCountByCodeToken(String codeToken);
}