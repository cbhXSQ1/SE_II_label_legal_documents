package com.seii.legal.data.entity;

import com.seii.legal.common.enums.ApplicationStatus;

import java.time.LocalDateTime;

public record ApprovalRecord(
        String approvalId,
        String applicationId,
        String approverId,
        LocalDateTime approvedAt,
        ApplicationStatus result,
        String note
) {
}
