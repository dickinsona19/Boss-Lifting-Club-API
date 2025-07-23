package com.BossLiftingClub.BossLifting.Trainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrainerServiceImpl implements TrainerService {
    @Autowired
    private TrainerRepository trainerRepository;

    @Override
    public List<Trainer> getAllTrainers() {
        return trainerRepository.findAll();
    }

    @Override
    public Trainer addTrainer(Trainer trainer) {
        return trainerRepository.save(trainer);
    }

    @Override
    public void deleteTrainer(Integer id) {
        if (!trainerRepository.existsById(id)) {
            throw new RuntimeException("Trainer not found");
        }
        trainerRepository.deleteById(id);
    }

    @Override
    public Trainer updateSessionCounter(Integer id, Integer sessionCounter) {
        Trainer trainer = trainerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trainer not found"));
        trainer.setSessionCounter(sessionCounter);
        return trainerRepository.save(trainer);
    }
}