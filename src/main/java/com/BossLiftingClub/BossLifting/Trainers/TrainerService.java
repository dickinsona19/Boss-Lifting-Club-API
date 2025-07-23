package com.BossLiftingClub.BossLifting.Trainers;

import java.util.List;

public interface TrainerService {
    List<Trainer> getAllTrainers();
    Trainer addTrainer(Trainer trainer);
    void deleteTrainer(Integer id);
    Trainer updateSessionCounter(Integer id, Integer sessionCounter);
}