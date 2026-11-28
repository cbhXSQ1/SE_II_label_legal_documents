package com.seii.legal.data.entity;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.ApplicationStatus;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import java.time.LocalDateTime;
import java.util.List;

public record AccountApplication(
        String applicationId,
        String account,
        String password,
        String name,
        String unit,
        SystemType system,
        IdentityType identity,
        ApplicationStatus status,
        LocalDateTime appliedAt,
        String approverId,
        LocalDateTime approvedAt,
        String note
) {
}
