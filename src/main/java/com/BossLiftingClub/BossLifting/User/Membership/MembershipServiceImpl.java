package com.BossLiftingClub.BossLifting.User.Membership;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MembershipServiceImpl implements MembershipService {

    @Autowired
    private MembershipRepository membershipRepository;

    @Override
    public Membership addMembership(Membership membership) {
        return membershipRepository.save(membership);
    }

    @Override
    public List<Membership> getAllMemberships() {
        return membershipRepository.findAll();
    }

    @Override
    public Membership getMembershipById(Long id) {
        Optional<Membership> membership = membershipRepository.findById(id);
        return membership.orElse(null);
    }
}