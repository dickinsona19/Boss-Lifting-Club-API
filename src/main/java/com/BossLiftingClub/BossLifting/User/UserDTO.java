package com.BossLiftingClub.BossLifting.User;

import java.time.LocalDateTime;
import java.util.Base64;

public class UserDTO {
    private Long id;
    private String phoneNumber;
    private String profilePictureBase64; // Base64-encoded profile picture
    private String signatureDataBase64; // Base64-encoded signature data
    private LocalDateTime waiverSignedDate;

    public UserDTO(User user) {
        this.id = user.getId();
        this.phoneNumber = user.getPhoneNumber();
        this.waiverSignedDate = user.getWaiverSignedDate();
        // Convert byte[] to Base64 for JSON response
        this.profilePictureBase64 = user.getProfilePicture() != null
                ? Base64.getEncoder().encodeToString(user.getProfilePicture())
                : null;
        this.signatureDataBase64 = user.getSignatureData() != null
                ? Base64.getEncoder().encodeToString(user.getSignatureData())
                : null;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getProfilePictureBase64() {
        return profilePictureBase64;
    }

    public void setProfilePictureBase64(String profilePictureBase64) {
        this.profilePictureBase64 = profilePictureBase64;
    }

    public String getSignatureDataBase64() {
        return signatureDataBase64;
    }

    public void setSignatureDataBase64(String signatureDataBase64) {
        this.signatureDataBase64 = signatureDataBase64;
    }

    public LocalDateTime getWaiverSignedDate() {
        return waiverSignedDate;
    }

    public void setWaiverSignedDate(LocalDateTime waiverSignedDate) {
        this.waiverSignedDate = waiverSignedDate;
    }
}