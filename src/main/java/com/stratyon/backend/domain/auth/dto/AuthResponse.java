package com.stratyon.backend.domain.auth.dto;

import java.util.UUID;

public record AuthResponse(
        UUID id,
        String email,
        String companyName,
        UUID firmId,
        String token
) {}
