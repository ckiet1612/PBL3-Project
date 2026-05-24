package com.pbl3.project.pbl3_project.dto;

public record UserRegistrationRequest(
    String username,
    String password,
    String fullName
) {
}
