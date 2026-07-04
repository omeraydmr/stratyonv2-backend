package com.stratyon.backend.domain.notification;

import com.stratyon.backend.domain.auth.User;
import com.stratyon.backend.domain.auth.UserRepository;
import com.stratyon.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public List<Notification> listNotifications() {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUserId());
    }

    @Transactional
    public Notification markRead(UUID id) {
        Notification notification = notificationRepository.findByIdAndUserId(id, currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead() {
        notificationRepository.markAllReadByUserId(currentUserId());
    }

    public void createForUser(User user, String title, String body, String type, String href) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .title(title)
                .body(body)
                .type(type)
                .href(href)
                .build());
    }

    private UUID currentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."))
                .getId();
    }
}
