package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.ApiSessionDto;
import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiSessionServiceTest {

    @Test
    void issueSessionReturnsBearerTokenThatResolvesUser() {
        UserRepository userRepository = mock(UserRepository.class);
        User user = user(7L, true);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        ApiSessionService service = new ApiSessionService(userRepository, "test-session-secret", 30);
        ApiSessionDto session = service.issueSession(user);

        User resolved = service.requireUser("Bearer " + session.accessToken());

        assertEquals("Bearer", session.tokenType());
        assertEquals(7L, resolved.getId());
    }

    @Test
    void requireUserRejectsTamperedToken() {
        UserRepository userRepository = mock(UserRepository.class);
        User user = user(7L, true);
        ApiSessionService service = new ApiSessionService(userRepository, "test-session-secret", 30);
        ApiSessionDto session = service.issueSession(user);
        String tampered = session.accessToken().replaceFirst(".$", "x");

        assertThrows(ApiAuthenticationException.class, () -> service.requireUser("Bearer " + tampered));
    }

    @Test
    void requireUserRejectsDisabledUser() {
        UserRepository userRepository = mock(UserRepository.class);
        User user = user(7L, true);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, false)));

        ApiSessionService service = new ApiSessionService(userRepository, "test-session-secret", 30);
        ApiSessionDto session = service.issueSession(user);

        assertThrows(ApiAuthenticationException.class, () -> service.requireUser("Bearer " + session.accessToken()));
    }

    private User user(Long id, boolean enabled) {
        User user = new User();
        user.setId(id);
        user.setUsername("api-user");
        user.setFullName("API User");
        user.setRole(Role.ADMIN);
        user.setEnabled(enabled);
        return user;
    }
}
