package com.BossLiftingClub.BossLifting.User;


import java.util.Set;

public class UserDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Boolean isInGoodStanding;
    private String entryQrcodeToken;
    private String userStripeMemberId;
    private String referralCode;
    private Set<ReferredUserDto> referredMembersDto;

    public UserDTO() {}

    public UserDTO(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.phoneNumber = user.getPhoneNumber();
        this.isInGoodStanding = user.getIsInGoodStanding();
        this.entryQrcodeToken = user.getEntryQrcodeToken();
        this.userStripeMemberId = user.getUserStripeMemberId();
        this.referralCode = user.getReferralCode();
        this.referredMembersDto = user.getReferredMembersDto();  // safe: this avoids the LOBs
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Boolean getIsInGoodStanding() {
        return isInGoodStanding;
    }

    public void setIsInGoodStanding(Boolean isInGoodStanding) {
        this.isInGoodStanding = isInGoodStanding;
    }

    public String getEntryQrcodeToken() {
        return entryQrcodeToken;
    }

    public void setEntryQrcodeToken(String entryQrcodeToken) {
        this.entryQrcodeToken = entryQrcodeToken;
    }

    public String getUserStripeMemberId() {
        return userStripeMemberId;
    }

    public void setUserStripeMemberId(String userStripeMemberId) {
        this.userStripeMemberId = userStripeMemberId;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public Set<ReferredUserDto> getReferredMembersDto() {
        return referredMembersDto;
    }

    public void setReferredMembersDto(Set<ReferredUserDto> referredMembersDto) {
        this.referredMembersDto = referredMembersDto;
    }
}
