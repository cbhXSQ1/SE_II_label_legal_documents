package com.seii.legal.data.entity;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.VersionType;
import java.time.LocalDateTime;
import java.util.List;

public record StudentAccountResult(
        String accountId,
        String initialPassword,
        String setPasswordLink,
        boolean created
) {
}
