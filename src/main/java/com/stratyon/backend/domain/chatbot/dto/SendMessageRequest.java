package com.stratyon.backend.domain.chatbot.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record SendMessageRequest(
        @NotBlank String content,
        UUID firmId
) {}
