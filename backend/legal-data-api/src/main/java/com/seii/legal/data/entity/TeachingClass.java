package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record TeachingClass(
        String classId,
        String classCode,
        String name,
        String teacherId,
        String term,
        LocalDateTime createdAt
) {
}
