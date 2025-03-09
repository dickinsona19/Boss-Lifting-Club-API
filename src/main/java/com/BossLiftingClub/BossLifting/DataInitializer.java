package com.BossLiftingClub.BossLifting;

import com.BossLiftingClub.BossLifting.User.User;
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

            // Insert sample users
            if (userRepository.count() == 0) {
                UserTitles founding = userTitleRepository.findByTitle("Founding User").get();
                UserTitles newUser = userTitleRepository.findByTitle("New User").get();
                userRepository.save(new User("Alice", "Smith", "AlicePass321", "9876543210", "12345", founding));
                userRepository.save(new User("Bob", "Brown", "BobSecure456", "4567891230", "11111", newUser));
                System.out.println("Inserted sample users");
            }
        };
    }
}