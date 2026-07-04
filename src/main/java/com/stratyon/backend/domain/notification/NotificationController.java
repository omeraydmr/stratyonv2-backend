package com.stratyon.backend.domain.notification;

import com.stratyon.backend.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> listNotifications() {
        return ResponseEntity.ok(ApiResponse.of(notificationService.listNotifications()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markRead(@PathVariable UUID id) {
        Notification n = notificationService.markRead(id);
        return ResponseEntity.ok(ApiResponse.of(Map.of("id", n.getId().toString(), "read", n.isRead())));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.ok(ApiResponse.message("All notifications marked as read."));
    }
}
