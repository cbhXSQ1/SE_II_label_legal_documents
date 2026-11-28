package com.seii.legal.data.entity;

import java.time.LocalDateTime;

public record Notification(
        String notificationId,
        String receiverId,
        String content,
        String type,
        boolean read,
        LocalDateTime sentAt
) {
}
