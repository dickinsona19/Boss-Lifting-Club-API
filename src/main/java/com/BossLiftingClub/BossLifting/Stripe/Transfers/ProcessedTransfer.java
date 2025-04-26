package com.BossLiftingClub.BossLifting.Stripe.Transfers;

import jakarta.persistence.Entity;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Entity
public class ProcessedTransfer {
    @Id
    private String chargeId;
    private String invoiceId;
    private String transferId;
    private LocalDateTime createdAt;

    // Constructors
    public ProcessedTransfer() {}
    public ProcessedTransfer(String chargeId, String invoiceId, String transferId, LocalDateTime createdAt) {
        this.chargeId = chargeId;
        this.invoiceId = invoiceId;
        this.transferId = transferId;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getChargeId() {
        return chargeId;
    }

    public void setChargeId(String chargeId) {
        this.chargeId = chargeId;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}