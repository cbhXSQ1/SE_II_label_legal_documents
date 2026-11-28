package com.seii.legal.function.dto;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;

public record ApplyCommand(
        String name,
        String unit,
        SystemType system,
        IdentityType identity,
        String studentNo,
        String teacherNo,
        String note
) {
}
