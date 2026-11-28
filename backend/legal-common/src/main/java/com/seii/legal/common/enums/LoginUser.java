package com.seii.legal.common.enums;

import java.time.LocalDateTime;

public record LoginUser(
        String accountId,
        SystemType system,
        IdentityType identity,
        String credential,
        LocalDateTime expiresAt
) {
}
