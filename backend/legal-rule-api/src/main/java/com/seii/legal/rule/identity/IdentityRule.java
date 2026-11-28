package com.seii.legal.rule.identity;

import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.data.entity.Account;
import java.util.List;

public interface IdentityRule {
    boolean hasRole(Account account, IdentityType identity);

    DataScope resolveScope(LoginUser user, String taskId, String classId);

    boolean isEnabled(Account account);

    List<IdentityOption> availableIdentities(Account account);

    record IdentityOption(SystemType system, IdentityType identity, String description) {
    }
}
