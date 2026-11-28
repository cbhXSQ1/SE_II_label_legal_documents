package com.seii.legal.function.dto.student;

import java.time.LocalDateTime;
import java.util.List;

public record MaterialView(
        String id,
        String title,
        String type,
        String content,
        String guideVersion,
        List<String> points,
        String fileId,
        String classId,
        String className,
        String status,
        LocalDateTime publishedAt,
        boolean studied,
        String completionStatus,
        int durationSeconds,
        LocalDateTime readAt
) {
}
