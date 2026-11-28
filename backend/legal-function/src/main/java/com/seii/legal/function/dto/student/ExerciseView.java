package com.seii.legal.function.dto.student;

import java.time.LocalDateTime;

public record ExerciseView(
        String id,
        String name,
        LocalDateTime deadline,
        boolean allowRedo,
        String status,
        String completionStatus,
        long remainingSeconds,
        boolean overdue,
        LocalDateTime createdAt
) {
}
