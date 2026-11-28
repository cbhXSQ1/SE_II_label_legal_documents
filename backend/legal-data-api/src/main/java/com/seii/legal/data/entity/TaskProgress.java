package com.seii.legal.data.entity;

import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import java.time.LocalDateTime;

public record TaskProgress(
        int expectedCount,
        int submittedCount,
        int unsubmittedCount,
        int conflictCount,
        int confirmedCount,
        double averageScore
) {
}
