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

    public User register(String username, String password, String fullName) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setFullName(fullName);
        return register(user);
    }

    public User register(User user) {
        if (user == null) {
            throw new ValidationException("Registration request is required");
        }
        String username = cleanRequired(user.getUsername(), "Username");
        String password = cleanRequired(user.getPassword(), "Password");
        String fullName = cleanRequired(user.getFullName(), "Full name");
        if (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(Role.STAFF);
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

    private String cleanRequired(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(label + " is required");
        }
        return value.trim();
    }
}
