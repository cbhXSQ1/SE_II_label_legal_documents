package com.seii.legal.data.entity;

import java.time.LocalDateTime;

public record LearningRecord(
        String studentId,
        String materialId,
        LocalDateTime firstStudyAt,
        LocalDateTime lastStudyAt,
        long durationSeconds,
        String completionStatus,
        boolean confirmed
) {
}
