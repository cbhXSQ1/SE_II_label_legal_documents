package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record LearningRecord(
        String studentId,
        String materialId,
        LocalDateTime readAt,
        int durationSeconds
) {
}
