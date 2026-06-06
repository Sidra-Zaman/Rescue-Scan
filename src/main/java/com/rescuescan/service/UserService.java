package com.rescuescan.service;

import com.rescuescan.model.User;
import com.rescuescan.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setQrCode(UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        user.setProfileCompleted(false);

        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User findByQrCode(String qrCode) {
        return userRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid QR code"));
    }

    public User completeProfile(User user) {
        user.setProfileCompleted(true);
        return userRepository.save(user);
    }
}
