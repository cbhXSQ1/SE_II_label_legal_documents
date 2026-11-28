package com.seii.legal.function.dto;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.ApplicationStatus;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.rule.identity.AccountRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record ApplicationItem(
            String id,
            String name,
            String unit,
            SystemType system,
            IdentityType identity,
            String studentNo,
            String teacherNo,
            String note,
            String submittedAt,
            ApplicationStatus status
    ) {
    }

    public record ReviewApplicationRequest(
            @NotBlank(message = "申请标识不能为空") String applicationId,
            @NotNull(message = "审批动作不能为空") AccountRule.ApprovalAction action,
            List<IdentityType> roles,
            String note
    ) {
    }

    public record AccountItem(
            String id,
            String account,
            String name,
            String unit,
            List<String> roles,
            AccountStatus status
    ) {
    }

    public record UpdateAccountRolesRequest(
            @NotBlank(message = "账号标识不能为空") String accountId,
            List<IdentityType> roles,
            AccountStatus status,
            String reason
    ) {
    }

    public record GuideContentPayload(
            List<String> nonProps,
            List<String> props,
            List<String> relations,
            List<String> diagramRules
    ) {
    }

    public record ImportGuideRequest(
            String fileId,
            @NotBlank(message = "请填写版本号") String version,
            @NotBlank(message = "请填写适用范围") String scope,
            GuideContentPayload content
    ) {
    }

    public record ImportGuideResponse(String version) {
    }

    public record GuideVersionItem(
            String version,
            String importedAt,
            String operator,
            String scope,
            String status
    ) {
    }

    public record GuideListResponse(
            List<GuideVersionItem> versions,
            String latest
    ) {
    }

    public record OverviewTaskItem(
            String id,
            String name,
            String publisher,
            String publishedAt,
            String deadline,
            int due,
            int submitted,
            int unsubmitted,
            int conflictCount,
            int confirmedCount,
            Double avgScore
    ) {
    }

    public record OverviewResponse(
            long taskCount,
            long conflictCount,
            List<OverviewTaskItem> tasks
    ) {
    }
}
