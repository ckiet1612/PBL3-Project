package com.pbl3.project.pbl3_project.controller;

import com.pbl3.project.pbl3_project.dto.UserRegistrationRequest;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.ApiSessionService;
import com.pbl3.project.pbl3_project.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String REGISTRATION_DISABLED_MESSAGE =
        "Public registration API is disabled; manage accounts from an authenticated admin workspace";

    private final AuthService authService;
    private final ApiSessionService apiSessionService;
    private final boolean publicRegistrationEnabled;

    public AuthController(
        AuthService authService,
        ApiSessionService apiSessionService,
        @Value("${app.api.public-registration.enabled:false}") boolean publicRegistrationEnabled
    ) {
        this.authService = authService;
        this.apiSessionService = apiSessionService;
        this.publicRegistrationEnabled = publicRegistrationEnabled;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegistrationRequest request) {
        if (!publicRegistrationEnabled) {
            return ResponseEntity.status(403).body(REGISTRATION_DISABLED_MESSAGE);
        }
        return ResponseEntity.ok(apiSessionService.issueSession(authService.register(
            request.username(),
            request.password(),
            request.fullName()
        )));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        User user = authService.login(username, password);
        if (user != null) {
            return ResponseEntity.ok(apiSessionService.issueSession(user));
        }
        return ResponseEntity.status(401).body("Invalid username or password");
    }
}
