package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record LearningMaterial(
        String materialId,
        String title,
        String content,
        List<String> pointIds,
        String fileId,
        String classId,
        String status,
        String createdBy,
        LocalDateTime createdAt
) {
}
