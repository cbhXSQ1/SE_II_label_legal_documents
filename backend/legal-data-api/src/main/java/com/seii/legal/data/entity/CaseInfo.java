package com.seii.legal.data.entity;

import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import java.time.LocalDateTime;

public record CaseInfo(
        String caseNo,
        String caseName,
        String court,
        String judge,
        String parties,
        String cause,
        String filingDate,
        String extra
) {
}
