package com.seii.legal.data.entity;

import com.seii.legal.common.enums.RoundType;
import java.time.LocalDateTime;
import java.util.List;

public record ReadingRecord(
        String userId,
        String version,
        LocalDateTime readAt
) {
}
