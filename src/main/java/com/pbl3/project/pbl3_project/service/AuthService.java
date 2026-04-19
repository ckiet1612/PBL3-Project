package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(User user) {
        if (userRepository.findByUsernameIgnoreCase(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        user.setUsername(user.getUsername().trim());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole(Role.STAFF);
        }
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public User login(String username, String password) {
        Optional<User> optionalUser = userRepository.findByUsernameIgnoreCase(username);
        if (optionalUser.isEmpty()) {
            return null;
        }

        User user = optionalUser.get();
        if (!user.isEnabled()) {
            throw new AuthorizationException("This account has been disabled");
        }

        String storedPassword = user.getPassword();
        if (isBcryptHash(storedPassword)) {
            return passwordEncoder.matches(password, storedPassword) ? user : null;
        }

        if (storedPassword != null && storedPassword.equals(password)) {
            user.setPassword(passwordEncoder.encode(password));
            return userRepository.save(user);
        }

        return null;
    }

    private boolean isBcryptHash(String value) {
        return value != null && value.startsWith("$2");
    }
}
