package com.seii.legal.function.dto;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;

public record IdentityOptionView(
        SystemType system,
        IdentityType identity,
        String systemName,
        String identityName,
        String description
) {
}
