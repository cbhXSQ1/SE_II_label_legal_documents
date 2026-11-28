package com.seii.legal.data.repository;

import com.seii.legal.common.enums.ApplicationStatus;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.data.entity.AccountApplication;
import com.seii.legal.data.entity.ApprovalRecord;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ApplicationRepository {
    String save(AccountApplication application);

    /** 附录协同项：审批建号时按申请 ID 读取申请人信息。 */
    Optional<AccountApplication> findById(String applicationId);

    PageResult<AccountApplication> findPending(IdentityType approverIdentity, SystemType system, int page, int size);

    void updateStatus(String applicationId, ApplicationStatus status, String approverId, LocalDateTime time, String note);

    void saveApproval(ApprovalRecord record);
}
