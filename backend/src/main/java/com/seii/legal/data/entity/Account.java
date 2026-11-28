package com.seii.legal.data.entity;

import com.seii.legal.common.enums.AccountStatus;

import java.util.List;

public record Account(
        String accountId,
        String account,
        String password,
        String name,
        String unit,
        AccountStatus status,
        List<Role> roles
) {
}
