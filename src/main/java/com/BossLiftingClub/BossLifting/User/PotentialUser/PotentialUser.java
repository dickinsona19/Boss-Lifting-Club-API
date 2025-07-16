package com.BossLiftingClub.BossLifting.User.PotentialUser;

import com.BossLiftingClub.BossLifting.Promo.Promo;
import com.BossLiftingClub.BossLifting.Promo.PromoRepository;
import com.BossLiftingClub.BossLifting.User.FirebaseService;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

// Entity
@Entity
@Table(name = "potential_user") // Explicitly specify the table name
public class PotentialUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // Matches the database column name
    private Long id;

    @Column(name = "first_name") // Maps to first_name in the database
    private String firstName;

    @Column(name = "last_name") // Maps to last_name in the database
    private String lastName;

    @Column(name = "email") // Matches the database column name
    private String email;

    @Column(name = "waiver_signature") // Maps to waiver_signature in the database
    private String waiverSignature;

    @Column(name = "phone_number") // Maps to phone_number in the database
    private String phoneNumber;

    @Column(name = "has_reddemed_free_pass") // Maps to has_reddemed_free_pass in the database
    private boolean hasReddemedFreePass = false;

    // Default constructor
    public PotentialUser() {}

    // Parameterized constructor
    public PotentialUser(String firstName, String lastName, String email, String waiverSignature, String phoneNumber, boolean hasReddemedFreePass) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.waiverSignature = waiverSignature;
        this.phoneNumber = phoneNumber;
        this.hasReddemedFreePass = hasReddemedFreePass;
    }
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWaiverSignature() { return waiverSignature; }
    public void setWaiverSignature(String waiverSignature) { this.waiverSignature = waiverSignature; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isHasReddemedFreePass() { return hasReddemedFreePass; }
    public void setHasReddemedFreePass(boolean hasReddemedFreePass) { this.hasReddemedFreePass = hasReddemedFreePass; }
}

// Repository
interface PotentialUserRepository extends JpaRepository<PotentialUser, Long> {
    PotentialUser findByEmail(String email);
}

// Service
@Service
class PotentialUserService {
    @Autowired
    private PotentialUserRepository repository;
    @Autowired
    private PromoRepository promoRepository;
    private final PotentialUserRepository potentialUserRepository;

    public PotentialUserService(PotentialUserRepository potentialUserRepository) {
        this.potentialUserRepository = potentialUserRepository;
    }
    public List<PotentialUser> getAllUsers() {
        return repository.findAll();
    }

    public PotentialUser addUser(PotentialUser user, String promoCode) {
        // Check if a user with the same email already exists
        PotentialUser existingUser = repository.findByEmail(user.getEmail());
        if (existingUser != null) {
            return existingUser; // Don't add a new one, return the existing user
        }

        if (promoCode != null) {
            // Search for the promo
            Optional<Promo> optionalPromo = promoRepository.findByCodeToken(promoCode);
            if (optionalPromo.isPresent()) {
                Promo promo = optionalPromo.get();
                // Increment freePassCount if promo exists
                promo.setFreePassCount(promo.getFreePassCount() + 1);
                promoRepository.save(promo);
            }
        }

        // Save and return the new user
        return repository.save(user);
    }

    public void deleteUser(Long id) {
        repository.deleteById(id);
    }

    public Optional<PotentialUser> updateWaiverSignature(Long id, String imageUrl) {
        Optional<PotentialUser> potentialUserOpt = potentialUserRepository.findById(id);
        if (potentialUserOpt.isPresent()) {
            PotentialUser potentialUser = potentialUserOpt.get();
            potentialUser.setWaiverSignature(imageUrl); // Assuming a field like waiverSignature exists
            return Optional.of(potentialUserRepository.save(potentialUser));
        }
        return Optional.empty();
    }
    public Optional<PotentialUser> updateFreePassStatus(Long id, boolean hasRedeemedFreePass) {
        Optional<PotentialUser> potentialUserOpt = potentialUserRepository.findById(id);
        if (potentialUserOpt.isPresent()) {
            PotentialUser potentialUser = potentialUserOpt.get();
            potentialUser.setHasReddemedFreePass(hasRedeemedFreePass);
            return Optional.of(potentialUserRepository.save(potentialUser));
        }
        return Optional.empty();
    }
}

// Controller
@RestController
@RequestMapping("/api/potential-users")
class PotentialUserController {
    @Autowired
    private PotentialUserService service;

    private final FirebaseService firebaseService;
    public PotentialUserController(PotentialUserService potentialUserService, FirebaseService firebaseService) {
        this.service = potentialUserService;
        this.firebaseService = firebaseService;
    }
    @GetMapping
    public List<PotentialUser> getAllUsers() {
        return service.getAllUsers();
    }

    @PostMapping
    public PotentialUser addUser(@RequestBody PotentialUser user, @RequestParam(name = "from", required = false) @Nullable String promoCode) {
        return service.addUser(user, promoCode);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        service.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/waiver")
    public ResponseEntity<PotentialUser> saveWaiverSignature(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        try {
            // Upload to Firebase and get public URL
            String imageUrl = firebaseService.uploadImage(file);

            // Save the image URL as waiver signature for the potential user
            return service.updateWaiverSignature(id, imageUrl)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @PostMapping("/{id}/free-pass")
    public ResponseEntity<PotentialUser> redeemFreePass(@PathVariable Long id) {
        return service.updateFreePassStatus(id, true)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}