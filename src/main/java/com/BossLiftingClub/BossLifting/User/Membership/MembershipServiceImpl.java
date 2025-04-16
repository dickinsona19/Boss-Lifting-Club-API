package com.BossLiftingClub.BossLifting.User.Membership;

import jakarta.persistence.EntityNotFoundException;
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
    @Override
    public void deleteMembership(Long id) {
        membershipRepository.deleteById(id);
    }
    @Override
    public Membership updateMembershipPrice(Long id, String newPrice) {
        Membership membership = membershipRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found with id: " + id));

        membership.setPrice(newPrice);
        return membershipRepository.save(membership);
    }
}