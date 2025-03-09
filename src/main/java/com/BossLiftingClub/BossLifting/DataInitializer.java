package com.BossLiftingClub.BossLifting;

import com.BossLiftingClub.BossLifting.User.UserRepository;
import com.BossLiftingClub.BossLifting.User.UserTitles.UserTitles;
import com.BossLiftingClub.BossLifting.User.UserTitles.UserTitlesRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {
    @Bean
    public CommandLineRunner loadData(UserTitlesRepository userTitleRepository, UserRepository userRepository) {
        return args -> {
            // Insert user_titles
            if (userTitleRepository.count() == 0) {
                userTitleRepository.save(new UserTitles("Founding User"));
                userTitleRepository.save(new UserTitles("New User"));
                System.out.println("Inserted default user titles");
            }

        };
    }
}