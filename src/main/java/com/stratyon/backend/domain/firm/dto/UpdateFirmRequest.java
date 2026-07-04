package com.stratyon.backend.domain.firm.dto;

import java.util.List;

public record UpdateFirmRequest(
        String name,
        String industry,
        String size,
        List<String> goals
) {}
