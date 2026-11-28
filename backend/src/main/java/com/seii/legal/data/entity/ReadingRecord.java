package com.seii.legal.data.entity;

import java.time.LocalDateTime;

public record ReadingRecord(
        String userId,
        String version,
        LocalDateTime readAt,
        LocalDateTime confirmedAt,
        boolean confirmed
) {
}
