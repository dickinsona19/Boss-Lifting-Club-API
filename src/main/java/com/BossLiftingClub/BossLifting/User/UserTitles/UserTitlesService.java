package com.BossLiftingClub.BossLifting.User.UserTitles;

import java.util.List;
import java.util.Optional;

public interface UserTitlesService {
    List<UserTitles> getAllUserTitles();
    Optional<UserTitles> getUserTitleById(Long id);
    UserTitles createUserTitle(UserTitles userTitle);
    UserTitles updateUserTitle(Long id, UserTitles userTitle);
    void deleteUserTitle(Long id);
}
