package com.BossLiftingClub.BossLifting.Promo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/promos")
public class PromoController {

    @Autowired
    private PromoService promoService;

    @GetMapping
    public List<PromoDTO> getAllPromos() {
        return promoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromoDTO> getPromoById(@PathVariable Long id) {
        Optional<PromoDTO> promoDto = promoService.findById(id);
        return promoDto.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-token/{codeToken}")
    public ResponseEntity<PromoDTO> getPromoByCodeToken(@PathVariable String codeToken) {
        Optional<PromoDTO> promoDto = promoService.findByCodeToken(codeToken);
        return promoDto.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Promo createPromo(@RequestBody Promo promo) {
        return promoService.save(promo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Promo> updatePromo(@PathVariable Long id, @RequestBody Promo promoDetails) {
        Optional<Promo> promoOptional = promoService.findById(id).map(dto -> {
            Promo promo = new Promo();
            promo.setId(dto.getId());
            promo.setName(dto.getName());
            promo.setCodeToken(dto.getCodeToken());
            // Note: Users are not updated here to avoid lazy loading issues
            return promo;
        });
        if (promoOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Promo promo = promoOptional.get();
        promo.setName(promoDetails.getName());
        promo.setCodeToken(promoDetails.getCodeToken());
        promo.setUsers(promoDetails.getUsers());
        Promo updatedPromo = promoService.save(promo);
        return ResponseEntity.ok(updatedPromo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromo(@PathVariable Long id) {
        promoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/add-user")
    public ResponseEntity<String> addUserToPromo(@RequestParam String codeToken, @RequestParam Long userId) {
        try {
            promoService.addUserToPromo(codeToken, userId);
            return ResponseEntity.ok("User added to promo successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}