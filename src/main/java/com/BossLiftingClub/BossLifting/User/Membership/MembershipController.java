package com.BossLiftingClub.BossLifting.User.Membership;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memberships")
public class MembershipController {

    @Autowired
    private MembershipService membershipService;

    @PostMapping
    public ResponseEntity<Membership> addMembership(@RequestBody Membership membership) {
        Membership savedMembership = membershipService.addMembership(membership);
        return new ResponseEntity<>(savedMembership, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Membership>> getAllMemberships() {
        List<Membership> memberships = membershipService.getAllMemberships();
        return new ResponseEntity<>(memberships, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Membership> getMembershipById(@PathVariable Long id) {
        Membership membership = membershipService.getMembershipById(id);
        if (membership != null) {
            return new ResponseEntity<>(membership, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMembershipById(@PathVariable Long id) {
        Membership membership = membershipService.getMembershipById(id);
        if (membership != null) {
            membershipService.deleteMembership(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    @PutMapping("/{id}/price")
    public ResponseEntity<Membership> updateMembershipPrice(
            @PathVariable Long id,
            @RequestBody PriceUpdateRequest priceUpdateRequest) {
        try {
            Membership updatedMembership = membershipService.updateMembershipPrice(id, priceUpdateRequest.getPrice());
            return ResponseEntity.ok(updatedMembership);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
class PriceUpdateRequest {
    private String price;

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
}