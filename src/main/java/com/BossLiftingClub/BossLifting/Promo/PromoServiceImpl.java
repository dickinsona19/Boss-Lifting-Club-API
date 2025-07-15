package com.BossLiftingClub.BossLifting.Promo;

import com.BossLiftingClub.BossLifting.User.User;
import com.BossLiftingClub.BossLifting.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

import java.util.stream.Collectors;

@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoRepository promoRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<PromoDTO> findAll() {
        return promoRepository.findAll().stream()
                .map(promo -> new PromoDTO(
                        promo.getId(),
                        promo.getName(),
                        promo.getCodeToken(),
                        promo.getUsers() != null ?
                                promo.getUsers().stream().map(User::getId).collect(Collectors.toList()) :
                                List.of()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PromoDTO> findById(Long id) {
        return promoRepository.findById(id).map(promo -> new PromoDTO(
                promo.getId(),
                promo.getName(),
                promo.getCodeToken(),
                promo.getUsers() != null ?
                        promo.getUsers().stream().map(User::getId).collect(Collectors.toList()) :
                        List.of()
        ));
    }

    @Override
    public Optional<PromoDTO> findByCodeToken(String codeToken) {
        return promoRepository.findByCodeToken(codeToken).map(promo -> new PromoDTO(
                promo.getId(),
                promo.getName(),
                promo.getCodeToken(),
                promo.getUsers() != null ?
                        promo.getUsers().stream().map(User::getId).collect(Collectors.toList()) :
                        List.of()
        ));
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
        Optional<Promo> promoOptional = promoRepository.findByCodeToken(codeToken);
        if (promoOptional.isEmpty()) {
            throw new RuntimeException("Promo not found with codeToken: " + codeToken);
        }
        Promo promo = promoOptional.get();

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        User user = userOptional.get();

        promo.getUsers().add(user);
        save(promo);
    }
}