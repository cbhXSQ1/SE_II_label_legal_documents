package com.seii.legal.data.entity;

import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import java.time.LocalDateTime;

public record TaskChangeLog(
        String taskId,
        String operatorId,
        LocalDateTime time,
        String action,
        TaskStatus beforeStatus,
        TaskStatus afterStatus,
        String reason
) {
}
