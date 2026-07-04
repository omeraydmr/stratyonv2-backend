package com.stratyon.backend.domain.chatbot;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByFirmIdOrderByCreatedAtAsc(UUID firmId);
    List<ChatMessage> findByFirmIdOrderByCreatedAtDesc(UUID firmId, Pageable pageable);
}
