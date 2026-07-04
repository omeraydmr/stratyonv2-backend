package com.stratyon.backend.infrastructure.claude;

import java.util.List;

public record ClaudeResponse(List<Content> content) {
    public record Content(String type, String text) {}

    public String firstText() {
        if (content == null || content.isEmpty()) return "";
        return content.stream()
                .filter(c -> "text".equals(c.type()))
                .map(Content::text)
                .findFirst()
                .orElse("");
    }
}
