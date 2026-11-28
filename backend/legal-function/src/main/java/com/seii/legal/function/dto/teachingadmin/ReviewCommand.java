package com.seii.legal.function.dto.teachingadmin;

import com.seii.legal.common.enums.IdentityType;

public record ReviewCommand(
        String applicationId,
        String action,
        IdentityType identity,
        String note
) {
}
