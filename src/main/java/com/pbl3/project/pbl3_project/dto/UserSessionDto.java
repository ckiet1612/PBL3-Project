package com.pbl3.project.pbl3_project.dto;

import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;

public record UserSessionDto(
    Long id,
    String username,
    String fullName,
    Role role,
    boolean enabled
) {
    public static UserSessionDto from(User user) {
        if (user == null) {
            return null;
        }
        return new UserSessionDto(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getRole(),
            user.isEnabled()
        );
    }
}
