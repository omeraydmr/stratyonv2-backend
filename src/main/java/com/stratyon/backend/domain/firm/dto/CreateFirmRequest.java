package com.stratyon.backend.domain.firm.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateFirmRequest(
        @NotBlank String name,
        String industry,
        String size,
        List<String> goals
) {}
