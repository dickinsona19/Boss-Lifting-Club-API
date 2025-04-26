package com.BossLiftingClub.BossLifting.Stripe.Transfers;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedTransferRepository extends JpaRepository<ProcessedTransfer, String> {
    boolean existsByChargeId(String chargeId);
}