package com.BossLiftingClub.BossLifting.User.PotentialUser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.List;
import java.util.Optional;

// Entity
@Entity
class PotentialUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String waiverSignature;

    // Default constructor
    public PotentialUser() {}

    // Parameterized constructor
    public PotentialUser(String firstName, String lastName, String phoneNumber, String waiverSignature) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.waiverSignature = waiverSignature;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getWaiverSignature() { return waiverSignature; }
    public void setWaiverSignature(String waiverSignature) { this.waiverSignature = waiverSignature; }
}

// Repository
interface PotentialUserRepository extends JpaRepository<PotentialUser, Long> {}

// Service
@Service
class PotentialUserService {
    @Autowired
    private PotentialUserRepository repository;

    public List<PotentialUser> getAllUsers() {
        return repository.findAll();
    }

    public PotentialUser addUser(PotentialUser user) {
        return repository.save(user);
    }

    public void deleteUser(Long id) {
        repository.deleteById(id);
    }

    public PotentialUser updateWaiverSignature(Long id, String waiverSignature) {
        Optional<PotentialUser> optionalUser = repository.findById(id);
        if (optionalUser.isPresent()) {
            PotentialUser user = optionalUser.get();
            user.setWaiverSignature(waiverSignature);
            return repository.save(user);
        }
        return null;
    }
}

// Controller
@RestController
@RequestMapping("/api/potential-users")
class PotentialUserController {
    @Autowired
    private PotentialUserService service;

    @GetMapping
    public List<PotentialUser> getAllUsers() {
        return service.getAllUsers();
    }

    @PostMapping
    public PotentialUser addUser(@RequestBody PotentialUser user) {
        return service.addUser(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        service.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/waiver-signature")
    public ResponseEntity<PotentialUser> updateWaiverSignature(@PathVariable Long id, @RequestBody String waiverSignature) {
        PotentialUser updatedUser = service.updateWaiverSignature(id, waiverSignature);
        if (updatedUser != null) {
            return ResponseEntity.ok(updatedUser);
        }
        return ResponseEntity.notFound().build();
    }
}