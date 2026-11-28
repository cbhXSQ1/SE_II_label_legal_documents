package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record Exercise(
        String exerciseId,
        String classId,
        String teacherId,
        String name,
        List<String> questionIds,
        LocalDateTime deadline,
        String status,
        LocalDateTime createdAt
) {
}
