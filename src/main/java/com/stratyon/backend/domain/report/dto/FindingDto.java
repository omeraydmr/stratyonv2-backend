package com.stratyon.backend.domain.report.dto;

import java.util.UUID;

public record FindingDto(UUID id, String severity, String title, String detail) {}
