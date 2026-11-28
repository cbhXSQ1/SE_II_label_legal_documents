package com.seii.legal.function.dto.task;

import com.seii.legal.common.enums.TaskStatus;
import java.util.List;

public record TaskDetailView(
        String id,
        String name,
        String description,
        String deadline,
        String guideVersion,
        String documentId,
        List<String> annotators,
        String arbiter,
        TaskStatus status,
        String publisher,
        String publishedAt,
        String withdrawReason
) {
}
