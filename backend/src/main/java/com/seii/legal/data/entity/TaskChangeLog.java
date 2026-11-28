package com.seii.legal.data.entity;

import java.time.LocalDateTime;

public record TaskChangeLog(
        String logId,
        String taskId,
        String beforeContent,
        String afterContent,
        String reason,
        String operatorId,
        LocalDateTime operatedAt
) {
}
