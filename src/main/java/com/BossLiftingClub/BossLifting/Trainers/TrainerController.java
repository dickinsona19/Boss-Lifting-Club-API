package com.BossLiftingClub.BossLifting.Trainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainers")
public class TrainerController {
    @Autowired
    private TrainerService trainerService;

    @GetMapping
    public ResponseEntity<List<Trainer>> getAllTrainers() {
        return ResponseEntity.ok(trainerService.getAllTrainers());
    }

    @PostMapping
    public ResponseEntity<Trainer> addTrainer(@RequestBody Trainer trainer) {
        return ResponseEntity.ok(trainerService.addTrainer(trainer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrainer(@PathVariable Integer id) {
        trainerService.deleteTrainer(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/session-counter")
    public ResponseEntity<Trainer> updateSessionCounter(@PathVariable Integer id, @RequestParam Integer sessionCounter) {
        return ResponseEntity.ok(trainerService.updateSessionCounter(id, sessionCounter));
    }
}