package com.seii.legal.function.dto;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import java.util.List;

public record SessionView(
        String credential,
        String accountId,
        String name,
        String unit,
        SystemType system,
        IdentityType identity,
        String systemName,
        String identityName,
        boolean single,
        List<IdentityOptionView> options
) {
}
