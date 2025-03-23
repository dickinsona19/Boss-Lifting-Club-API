// ReferredUserDto.java
package com.BossLiftingClub.BossLifting.User;

import lombok.Data;

@Data
public class ReferredUserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String referralCode;

    // Constructor for mapping from User entity
    public ReferredUserDto(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.referralCode = user.getReferralCode();
    }
}