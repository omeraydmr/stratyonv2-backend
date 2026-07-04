package com.stratyon.backend.domain.chatbot;

import com.stratyon.backend.domain.chatbot.dto.SendMessageRequest;
import com.stratyon.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getHistory() {
        return ResponseEntity.ok(ApiResponse.of(chatbotService.getHistory()));
    }

    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatMessage>> sendMessage(@Valid @RequestBody SendMessageRequest req) {
        return ResponseEntity.ok(ApiResponse.of(chatbotService.sendMessage(req.content())));
    }
}
