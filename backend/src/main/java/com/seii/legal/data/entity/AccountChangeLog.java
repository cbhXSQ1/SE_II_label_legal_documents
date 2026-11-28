package com.seii.legal.data.entity;

import com.seii.legal.common.enums.AccountStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AccountChangeLog(
        String logId,
        String accountId,
        List<Role> beforeRoles,
        List<Role> afterRoles,
        AccountStatus beforeStatus,
        AccountStatus afterStatus,
        String reason,
        String operatorId,
        LocalDateTime operatedAt
) {
}
