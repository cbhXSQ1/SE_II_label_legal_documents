package com.seii.legal.data.entity;

import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import java.time.LocalDateTime;

public record TaskAssignment(
        String taskId,
        java.util.List<String> annotatorIds,
        String arbiterId,
        LocalDateTime assignedAt
) {
}
