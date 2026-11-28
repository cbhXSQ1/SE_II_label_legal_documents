package com.seii.legal.rule.identity;

import com.seii.legal.common.enums.ApplicationStatus;
import com.seii.legal.data.entity.Role;

import java.util.List;

public interface AccountRule {
    ApplicationStatus nextApplicationStatus(ApplicationStatus current, ApprovalAction action);

    boolean atLeastOneRole(List<Role> roles, Role removed);

    boolean canModifyOtherRole(String operatorId, String targetAccountId);

    enum ApprovalAction {
        APPROVE, RETURN, REJECT
    }
}
