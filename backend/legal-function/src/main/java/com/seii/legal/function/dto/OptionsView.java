package com.seii.legal.function.dto;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import java.util.List;

public record OptionsView(
        String name,
        List<IdentityOptionView> options,
        SystemType currentSystem,
        IdentityType currentIdentity
) {
}
