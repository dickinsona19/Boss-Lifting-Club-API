package com.BossLiftingClub.BossLifting.Promo;

import com.BossLiftingClub.BossLifting.User.User;
import com.BossLiftingClub.BossLifting.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoRepository promoRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<Promo> findAll() {
        return promoRepository.findAll();
    }

    @Override
    public Optional<Promo> findById(Long id) {
        return promoRepository.findById(id);
    }

    @Override
    public Optional<Promo> findByCodeToken(String codeToken) {
        return promoRepository.findByCodeToken(codeToken);
    }

    @Override
    public Promo save(Promo promo) {
        return promoRepository.save(promo);
    }

    @Override
    public void deleteById(Long id) {
        promoRepository.deleteById(id);
    }

    @Override
    public void addUserToPromo(String codeToken, Long userId) {
        Optional<Promo> promoOptional = findByCodeToken(codeToken);
        if (promoOptional.isEmpty()) {
            throw new RuntimeException("Promo not found with codeToken: " + codeToken);
        }
        Promo promo = promoOptional.get();

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        User user = userOptional.get();

        // Add user to promo's users list (assuming it's initialized)
        promo.getUsers().add(user);

        // Save the updated promo
        save(promo);
    }
}