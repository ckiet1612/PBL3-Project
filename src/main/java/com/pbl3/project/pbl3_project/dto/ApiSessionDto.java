package com.pbl3.project.pbl3_project.dto;

import java.time.LocalDateTime;

public record ApiSessionDto(
    String tokenType,
    String accessToken,
    LocalDateTime expiresAt,
    UserSessionDto user
) {
}
