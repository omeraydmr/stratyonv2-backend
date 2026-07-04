package com.stratyon.backend.domain.chatbot;

import com.stratyon.backend.domain.auth.User;
import com.stratyon.backend.domain.auth.UserRepository;
import com.stratyon.backend.domain.firm.Firm;
import com.stratyon.backend.domain.report.Report;
import com.stratyon.backend.domain.report.ReportRepository;
import com.stratyon.backend.infrastructure.claude.ClaudeClient;
import com.stratyon.backend.infrastructure.claude.ClaudeRequest;
import com.stratyon.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final ClaudeClient claudeClient;

    public List<ChatMessage> getHistory() {
        return chatMessageRepository.findByFirmIdOrderByCreatedAtAsc(requireFirmId());
    }

    @Transactional
    public ChatMessage sendMessage(String content) {
        User user = currentUser();
        Firm firm = user.getFirm();
        if (firm == null) throw new IllegalArgumentException("Complete the firm survey first.");

        // Persist user message
        ChatMessage userMessage = ChatMessage.builder()
                .firm(firm)
                .role("user")
                .content(content)
                .build();
        chatMessageRepository.save(userMessage);

        // Build conversation context
        String systemPrompt = buildSystemPrompt(firm);
        List<ClaudeRequest.Message> messages = buildMessageHistory(firm.getId(), content);

        // Call Claude
        String reply = claudeClient.complete(systemPrompt, messages);
        if (reply.isBlank()) reply = "I'm sorry, I couldn't process your request right now. Please try again.";

        // Persist assistant reply
        ChatMessage assistantMessage = ChatMessage.builder()
                .firm(firm)
                .role("assistant")
                .content(reply)
                .build();
        return chatMessageRepository.save(assistantMessage);
    }

    private String buildSystemPrompt(Firm firm) {
        List<Report> recentReports = reportRepository.findByFirmIdAndStatusOrderByCompletedAtDesc(
                firm.getId(), "completed");

        StringBuilder context = new StringBuilder();
        context.append("You are a strategic advisor for ")
                .append(firm.getName())
                .append(", a ")
                .append(firm.getIndustry() != null ? firm.getIndustry() : "")
                .append(" company.\n\n");

        if (!recentReports.isEmpty()) {
            context.append("Recent report summaries:\n");
            recentReports.stream().limit(5).forEach(r -> {
                context.append("- ").append(r.getTitle())
                        .append(" (Score: ").append(r.getScore()).append("/100): ")
                        .append(r.getSummary() != null ? r.getSummary() : "No summary")
                        .append("\n");

                r.getFindings().stream().limit(3).forEach(f ->
                        context.append("  Finding [").append(f.getSeverity()).append("]: ")
                                .append(f.getTitle()).append("\n"));
            });
        }

        context.append("\nProvide concise, data-driven strategic advice based on the above context.");
        return context.toString();
    }

    private List<ClaudeRequest.Message> buildMessageHistory(java.util.UUID firmId, String latestUserContent) {
        List<ChatMessage> history = chatMessageRepository.findByFirmIdOrderByCreatedAtDesc(
                firmId, PageRequest.of(0, 20));
        Collections.reverse(history);

        List<ClaudeRequest.Message> messages = new ArrayList<>();
        for (ChatMessage msg : history) {
            messages.add(new ClaudeRequest.Message(msg.getRole(), msg.getContent()));
        }
        // Add the current user message
        messages.add(new ClaudeRequest.Message("user", latestUserContent));
        return messages;
    }

    private java.util.UUID requireFirmId() {
        Firm firm = currentUser().getFirm();
        if (firm == null) throw new ResourceNotFoundException("Firm not found.");
        return firm.getId();
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}
