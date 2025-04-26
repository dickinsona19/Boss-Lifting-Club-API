package com.BossLiftingClub.BossLifting.Stripe.Transfers;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TransferService {
    private final ProcessedTransferRepository processedTransferRepository;

    public TransferService(ProcessedTransferRepository processedTransferRepository) {
        this.processedTransferRepository = processedTransferRepository;
    }

    public boolean hasProcessedCharge(String chargeId) {
        return processedTransferRepository.existsByChargeId(chargeId);
    }

    @Transactional
    public void saveTransfer(String chargeId, String invoiceId, String transferId) {
        processedTransferRepository.save(new ProcessedTransfer(chargeId, invoiceId, transferId, LocalDateTime.now()));
    }
}