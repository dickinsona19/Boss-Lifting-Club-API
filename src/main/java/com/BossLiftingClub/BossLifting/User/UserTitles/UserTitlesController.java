package com.BossLiftingClub.BossLifting.User.UserTitles;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usertitles")
public class UserTitlesController {

    private final UserTitlesService userTitlesService;

    @Autowired
    public UserTitlesController(UserTitlesService userTitlesService) {
        this.userTitlesService = userTitlesService;
    }

    @GetMapping
    public ResponseEntity<List<UserTitles>> getAllUserTitles() {
        List<UserTitles> userTitles = userTitlesService.getAllUserTitles();
        return ResponseEntity.ok(userTitles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserTitles> getUserTitleById(@PathVariable Long id) {
        return userTitlesService.getUserTitleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserTitles> createUserTitle(@RequestBody UserTitles userTitle) {
        UserTitles created = userTitlesService.createUserTitle(userTitle);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserTitles> updateUserTitle(@PathVariable Long id, @RequestBody UserTitles userTitle) {
        try {
            UserTitles updated = userTitlesService.updateUserTitle(id, userTitle);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserTitle(@PathVariable Long id) {
        userTitlesService.deleteUserTitle(id);
        return ResponseEntity.noContent().build();
    }
}
