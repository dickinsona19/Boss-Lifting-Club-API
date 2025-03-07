package com.BossLiftingClub.BossLifting.User.UserTitles;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserTitlesServiceImpl implements UserTitlesService {

    private final UserTitlesRepository userTitlesRepository;

    @Autowired
    public UserTitlesServiceImpl(UserTitlesRepository userTitlesRepository) {
        this.userTitlesRepository = userTitlesRepository;
    }

    @Override
    public List<UserTitles> getAllUserTitles() {
        return userTitlesRepository.findAll();
    }

    @Override
    public Optional<UserTitles> getUserTitleById(Long id) {
        return userTitlesRepository.findById(id);
    }

    @Override
    public UserTitles createUserTitle(UserTitles userTitle) {
        return userTitlesRepository.save(userTitle);
    }

    @Override
    public UserTitles updateUserTitle(Long id, UserTitles userTitle) {
        return userTitlesRepository.findById(id)
                .map(existing -> {
                    existing.setTitle(userTitle.getTitle());
                    return userTitlesRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("User title not found with id " + id));
    }

    @Override
    public void deleteUserTitle(Long id) {
        userTitlesRepository.deleteById(id);
    }
}
