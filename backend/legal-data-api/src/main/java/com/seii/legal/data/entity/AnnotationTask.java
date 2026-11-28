package com.seii.legal.data.entity;

import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;

public record AnnotationTask(
        String taskId,
        SystemType system,
        String name,
        String description,
        LocalDateTime deadline,
        String guideVersion,
        List<String> documentIds,
        List<String> annotatorIds,
        String arbiterId,
        TaskStatus status,
        boolean draftAllowed,
        String publisherId,
        LocalDateTime publishedAt,
        String withdrawReason
) {
}
