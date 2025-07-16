package com.BossLiftingClub.BossLifting.Promo;

import com.BossLiftingClub.BossLifting.User.User;
import com.BossLiftingClub.BossLifting.User.UserDTO;
import com.BossLiftingClub.BossLifting.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
        return promoRepository.findAllWithUsers().stream()
                .map(promo -> new PromoDTO(
                        promo.getId(),
                        promo.getName(),
                        promo.getCodeToken(),
                        promo.getUsers() != null ?
                                promo.getUsers().stream().map(UserDTO::new).toList() :
                                List.of(),
                        promo.getFreePassCount()
                ))
                .toList();
    }

    @Override
    public Optional<PromoDTO> findById(Long id) {
        return promoRepository.findByIdWithUsers(id).map(promo -> new PromoDTO(
                promo.getId(),
                promo.getName(),
                promo.getCodeToken(),
                promo.getUsers() != null ?
                        promo.getUsers().stream().map(UserDTO::new).toList() :
                        List.of(),
                promo.getFreePassCount()
        ));
    }

    @Override
    public Optional<PromoDTO> findByCodeToken(String codeToken) {
        return promoRepository.findByCodeTokenWithUsers(codeToken).map(promo -> new PromoDTO(
                promo.getId(),
                promo.getName(),
                promo.getCodeToken(),
                promo.getUsers() != null ?
                        promo.getUsers().stream().map(UserDTO::new).toList() :
                        List.of(),
                promo.getFreePassCount()
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
        Optional<Promo> promoOptional = promoRepository.findByCodeTokenWithUsers(codeToken);
        if (promoOptional.isEmpty()) {
            throw new RuntimeException("Promo not found with codeToken: " + codeToken);
        }
        Promo promo = promoOptional.get();

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        User user = userOptional.get();

        if (promo.getUsers() == null) {
            promo.setUsers(new ArrayList<>());
        }
        promo.getUsers().add(user);
        save(promo);
    }
}