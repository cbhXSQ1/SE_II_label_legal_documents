package com.seii.legal.function.dto;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.ApplicationStatus;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.rule.identity.AccountRule;
import java.util.List;

/**
 * 管理员端接口 DTO（ADMIN-01…07）。
 */
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
            String applicationId,
            AccountRule.ApprovalAction action,
            List<IdentityType> roles,
            String note
    ) {
    }

    public record AccountItem(
            String accountId,
            String account,
            String name,
            String unit,
            List<String> roles,
            AccountStatus status
    ) {
    }

    public record UpdateAccountRolesRequest(
            String accountId,
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
            String version,
            String scope,
            String fileId,
            GuideContentPayload content
    ) {
    }

    public record ImportGuideResponse(String version) {
    }

    public record GuideVersionItem(
            String version,
            String importedAt,
            String operatorName,
            String scope,
            String status
    ) {
    }

    public record GuideListResponse(
            List<GuideVersionItem> list,
            String latest
    ) {
    }

    public record OverviewTaskItem(
            String taskId,
            String name,
            String publisherName,
            String publishedAt,
            String deadline,
            int due,
            int submitted,
            int unsubmitted,
            int conflictCount,
            int confirmedCount,
            Double averageScore
    ) {
    }

    public record OverviewResponse(
            long totalTasks,
            long totalConflicts,
            List<OverviewTaskItem> tasks
    ) {
    }
}
