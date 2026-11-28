package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record Exercise(
        String exerciseId,
        String teacherId,
        String name,
        String description,
        LocalDateTime deadline,
        boolean allowRedo,
        String guideVersion,
        String classId,
        List<String> questionIds,
        String status,
        LocalDateTime publishedAt
) {
}
