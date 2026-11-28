package com.seii.legal.data.entity;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.ApplicationStatus;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import java.time.LocalDateTime;
import java.util.List;

public record Notification(
        String notificationId,
        String receiverId,
        String content,
        String type,
        LocalDateTime sentAt,
        boolean read
) {
}
