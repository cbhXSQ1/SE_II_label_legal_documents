package com.seii.legal.data.entity;

import com.seii.legal.common.enums.ApplicationStatus;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;

import java.time.LocalDateTime;

public record AccountApplication(
        String applicationId,
        String name,
        String unit,
        SystemType system,
        IdentityType identity,
        String studentNo,
        String teacherNo,
        String note,
        LocalDateTime submittedAt,
        ApplicationStatus status
) {
}
