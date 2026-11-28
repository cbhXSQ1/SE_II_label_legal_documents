package com.seii.legal.data.entity;

public record StudentAccountResult(
        String accountId,
        String initialPassword,
        String setPasswordLink,
        boolean created
) {
}
