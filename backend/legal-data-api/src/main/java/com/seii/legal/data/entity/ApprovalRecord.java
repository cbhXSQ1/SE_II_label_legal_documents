package com.seii.legal.data.entity;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.ApplicationStatus;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import java.time.LocalDateTime;
import java.util.List;

public record ApprovalRecord(
        String approvalId,
        String applicationId,
        String action,
        String operatorId,
        LocalDateTime time,
        String note
) {
}
