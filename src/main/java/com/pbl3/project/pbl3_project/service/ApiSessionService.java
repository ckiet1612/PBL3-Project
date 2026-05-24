package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.ApiSessionDto;
import com.pbl3.project.pbl3_project.dto.UserSessionDto;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

@Service
public class ApiSessionService {
    private static final Logger log = LoggerFactory.getLogger(ApiSessionService.class);
    private static final String TOKEN_VERSION = "v1";
    private static final String TOKEN_TYPE = "Bearer";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] signingSecret;
    private final long ttlSeconds;
    private final boolean ephemeralSecret;

    public ApiSessionService(
        UserRepository userRepository,
        @Value("${app.api.session.secret:}") String configuredSecret,
        @Value("${app.api.session.ttl-minutes:720}") long ttlMinutes
    ) {
        this.userRepository = userRepository;
        String normalizedSecret = configuredSecret == null ? "" : configuredSecret.trim();
        this.ephemeralSecret = normalizedSecret.isBlank();
        this.signingSecret = ephemeralSecret ? generateEphemeralSecret() : normalizedSecret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = Math.max(60, ttlMinutes * 60);
    }

    @PostConstruct
    void logSecretMode() {
        if (ephemeralSecret) {
            log.warn("app.api.session.secret is not configured; API tokens will be invalid after application restart");
        }
    }

    public ApiSessionDto issueSession(User user) {
        if (user == null || user.getId() == null || !user.isEnabled()) {
            throw new ApiAuthenticationException("Cannot issue API token for this user");
        }
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + ttlSeconds;
        String nonce = randomNonce();
        String payload = base64Url(String.join(
            "|",
            TOKEN_VERSION,
            String.valueOf(user.getId()),
            String.valueOf(issuedAt),
            String.valueOf(expiresAt),
            nonce
        ).getBytes(StandardCharsets.UTF_8));
        String token = payload + "." + sign(payload);
        return new ApiSessionDto(
            TOKEN_TYPE,
            token,
            LocalDateTime.ofInstant(Instant.ofEpochSecond(expiresAt), ZoneId.systemDefault()),
            UserSessionDto.from(user)
        );
    }

    public User requireUser(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        String[] tokenParts = token.split("\\.", -1);
        if (tokenParts.length != 2 || tokenParts[0].isBlank() || tokenParts[1].isBlank()) {
            throw new ApiAuthenticationException("Invalid API token");
        }
        String payload = tokenParts[0];
        String expectedSignature = sign(payload);
        if (!MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8),
            tokenParts[1].getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ApiAuthenticationException("Invalid API token signature");
        }

        String[] fields;
        try {
            fields = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8).split("\\|", -1);
        } catch (IllegalArgumentException ex) {
            throw new ApiAuthenticationException("Invalid API token");
        }
        if (fields.length != 5 || !TOKEN_VERSION.equals(fields[0])) {
            throw new ApiAuthenticationException("Unsupported API token");
        }
        Long userId = parseUserId(fields[1]);
        long expiresAt = parseEpoch(fields[3]);
        if (Instant.now().getEpochSecond() >= expiresAt) {
            throw new ApiAuthenticationException("API token expired");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiAuthenticationException("API token user no longer exists"));
        if (!user.isEnabled()) {
            throw new ApiAuthenticationException("API token user is disabled");
        }
        return user;
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new ApiAuthenticationException("Missing Authorization bearer token");
        }
        String trimmed = authorizationHeader.trim();
        if (!trimmed.regionMatches(true, 0, TOKEN_TYPE + " ", 0, TOKEN_TYPE.length() + 1)) {
            throw new ApiAuthenticationException("Authorization header must use Bearer token");
        }
        String token = trimmed.substring(TOKEN_TYPE.length()).trim();
        if (token.isBlank()) {
            throw new ApiAuthenticationException("Missing Authorization bearer token");
        }
        return token;
    }

    private Long parseUserId(String rawValue) {
        try {
            long id = Long.parseLong(rawValue);
            if (id <= 0) {
                throw new NumberFormatException("non-positive");
            }
            return id;
        } catch (NumberFormatException ex) {
            throw new ApiAuthenticationException("Invalid API token user");
        }
    }

    private long parseEpoch(String rawValue) {
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException ex) {
            throw new ApiAuthenticationException("Invalid API token expiry");
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret, HMAC_ALGORITHM));
            return base64Url(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not sign API token", ex);
        }
    }

    private String randomNonce() {
        byte[] bytes = new byte[18];
        secureRandom.nextBytes(bytes);
        return base64Url(bytes);
    }

    private byte[] generateEphemeralSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
