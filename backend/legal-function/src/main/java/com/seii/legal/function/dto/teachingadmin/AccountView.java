package com.seii.legal.function.dto.teachingadmin;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.IdentityType;

public record AccountView(
        String id,
        String account,
        String name,
        IdentityType identity,
        AccountStatus status,
        String className
) {
}
