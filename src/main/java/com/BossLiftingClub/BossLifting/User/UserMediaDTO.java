package com.BossLiftingClub.BossLifting.User;

import java.time.LocalDateTime;
import java.util.Base64;

public class UserMediaDTO {
    private String profilePicture;
    private String signatureData;
    private LocalDateTime waiverSignedDate;

    public UserMediaDTO(User user) {
        this.profilePicture = user.getProfilePictureUrl();
        this.signatureData = user.getSignatureData();
        this.waiverSignedDate = user.getWaiverSignedDate();
    }

    // Getters and setters
    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getSignatureData() {
        return signatureData;
    }

    public void setSignatureData(String signatureData) {
        this.signatureData = signatureData;
    }

    public LocalDateTime getWaiverSignedDate() {
        return waiverSignedDate;
    }

    public void setWaiverSignedDate(LocalDateTime waiverSignedDate) {
        this.waiverSignedDate = waiverSignedDate;
    }
}