package com.sindoflow.ops.auth;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UserEntity> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public UserEntity findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public UserEntity findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));
    }

    @Transactional
    public UserEntity create(String email, String password, String firstName, String lastName, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use: " + email);
        }

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setActive(true);

        log.info("Creating user: {} with role {}", email, role);
        return userRepository.save(user);
    }

    @Transactional
    public UserEntity update(UUID id, String email, String firstName, String lastName) {
        UserEntity user = findById(id);

        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Email already in use: " + email);
            }
            user.setEmail(email);
        }
        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }

        return userRepository.save(user);
    }

    @Transactional
    public UserEntity updateRole(UUID id, Role role) {
        UserEntity user = findById(id);
        user.setRole(role);
        log.info("Updated role for user {} to {}", id, role);
        return userRepository.save(user);
    }

    @Transactional
    public UserEntity updateStatus(UUID id, boolean active) {
        UserEntity user = findById(id);
        user.setActive(active);
        log.info("Updated status for user {} to active={}", id, active);
        return userRepository.save(user);
    }

    @Transactional
    public void softDelete(UUID id) {
        UserEntity user = findById(id);
        user.setActive(false);
        userRepository.save(user);
        log.info("Soft-deleted user: {}", id);
    }

    @Transactional
    public void resetPassword(UUID id, String newPassword) {
        UserEntity user = findById(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset for user: {}", id);
    }
}
