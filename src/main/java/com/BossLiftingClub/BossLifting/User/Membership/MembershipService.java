package com.BossLiftingClub.BossLifting.User.Membership;

import java.util.List;

public interface MembershipService {
    Membership addMembership(Membership membership);
    List<Membership> getAllMemberships();
    Membership getMembershipById(Long id);
}