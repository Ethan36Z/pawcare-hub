package com.pawcarehub.backend.service;

import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRoleNormalizationService implements ApplicationRunner {

    private final UserRepository userRepository;

    public UserRoleNormalizationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        userRepository.findAll().forEach(this::normalizeRole);
    }

    private void normalizeRole(User user) {
        String normalizedRole = UserRoles.normalizeLegacyRole(user.getRole());
        if (!normalizedRole.equals(user.getRole())) {
            user.setRole(normalizedRole);
            userRepository.save(user);
        }
    }
}
