package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record TaskAssignment(
        String taskId,
        List<String> annotatorIds,
        String arbiterId,
        LocalDateTime assignedAt
) {
}
