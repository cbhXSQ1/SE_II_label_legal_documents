package com.seii.legal.function.dto;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.rule.identity.AccountRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class TeachingAdminDtos {

    private TeachingAdminDtos() {
    }

    public record ImportRosterRequest(
            @NotBlank(message = "请先上传名单文件") String fileId
    ) {
    }

    public record ImportRosterResponse(
            String importId,
            int classCount,
            int studentCount,
            List<String> duplicateStudentNos,
            List<Integer> missingRows,
            List<String> unmatchedTeachers
    ) {
    }

    public record FailedRowItem(
            int row,
            String reason
    ) {
    }

    public record ImportResultResponse(
            int successClasses,
            int successStudents,
            List<FailedRowItem> failedRows,
            String downloadUrl
    ) {
    }

    public record ClassItem(
            String id,
            String code,
            String name,
            String term,
            String teacher,
            int studentCount
    ) {
    }

    public record TeachingAccountItem(
            String id,
            String account,
            String name,
            String identity,
            AccountStatus status,
            String className
    ) {
    }

    public record ReviewTeachingApplicationRequest(
            @NotBlank(message = "申请标识不能为空") String applicationId,
            @NotNull(message = "审批动作不能为空") AccountRule.ApprovalAction action,
            IdentityType identity,
            String note
    ) {
    }
}
