package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record LearningMaterial(
        String materialId,
        String teacherId,
        String title,
        String type,
        String content,
        String fileId,
        String guideVersion,
        List<String> pointIds,
        String classId,
        String status,
        LocalDateTime publishedAt
) {
}
