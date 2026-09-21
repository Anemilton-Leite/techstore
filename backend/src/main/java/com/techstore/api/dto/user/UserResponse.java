package com.techstore.api.dto.user;

public record UserResponse(
        Long id,
        String name,
        String email,
        String role
) {}
