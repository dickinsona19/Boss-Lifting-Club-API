package com.BossLiftingClub.BossLifting.User;

import com.BossLiftingClub.BossLifting.User.Membership.Membership;
import com.BossLiftingClub.BossLifting.User.UserTitles.UserTitles;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Works with AUTO_INCREMENT in MySQL/PostgreSQL
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @Column(name = "is_in_good_standing", nullable = false)
    private Boolean isInGoodStanding = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "entry_qrcode_token", nullable = false, unique = true)
    private String entryQrcodeToken;

    @Column(name = "user_stripe_member_id", unique = true)
    private String userStripeMemberId;

    @ManyToOne(fetch = FetchType.EAGER) // Default, but explicit here
    @JoinColumn(name = "user_title_id", referencedColumnName = "id")
    private UserTitles userTitles;

    @Lob
    @Column(name = "signature_data")
    private byte[] signatureData;

    @Column(name = "waiver_signed_date")
    private LocalDateTime waiverSignedDate;

    @Lob // Large Object for binary data
    private byte[] profilePicture;
    // Default constructor required by JPA


    @ManyToOne
    @JoinColumn(name = "membership_id")
    private Membership membership;

    public User() {
    }

    // Constructor with fields (excluding id, which is auto-generated)
    public User(String firstName, String lastName, String password, String phoneNumber,
                Boolean isInGoodStanding, String entryQrcodeToken, String userStripeMemberId,
                UserTitles userTitles) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.isInGoodStanding = isInGoodStanding;
        this.createdAt = LocalDateTime.now();
        this.entryQrcodeToken = entryQrcodeToken;
        this.userStripeMemberId = userStripeMemberId;
        this.userTitles = userTitles;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }
    // No setter for id since it’s auto-generated

    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Boolean getIsInGoodStanding() {
        return isInGoodStanding;
    }
    public void setIsInGoodStanding(Boolean inGoodStanding) {
        this.isInGoodStanding = inGoodStanding;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getEntryQrcodeToken() { return entryQrcodeToken; }
    public void setEntryQrcodeToken(String entryQrcodeToken) { this.entryQrcodeToken = entryQrcodeToken; }

    public String getUserStripeMemberId() {
        return userStripeMemberId;
    }
    public void setUserStripeMemberId(String userStripeMemberId) {
        this.userStripeMemberId = userStripeMemberId;
    }

    public UserTitles getUserTitles() {
        return userTitles;
    }
    public void setUserTitles(UserTitles userTitles) {
        this.userTitles = userTitles;
    }
    public byte[] getProfilePicture() { return profilePicture; }
    public void setProfilePicture(byte[] profilePicture) { this.profilePicture = profilePicture; }
    public byte[] getSignatureData() { return signatureData; }
    public void setSignatureData(byte[] signatureData) { this.signatureData = signatureData; }

    public LocalDateTime getWaiverSignedDate() { return waiverSignedDate; }
    public void setWaiverSignedDate(LocalDateTime waiverSignedDate) { this.waiverSignedDate = waiverSignedDate; }

    public Membership getMembership() { return membership; }
    public void setMembership(Membership membership) { this.membership = membership; }
}