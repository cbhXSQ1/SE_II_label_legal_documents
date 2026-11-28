package com.seii.legal.function.service;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.TaskStatus;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 状态/策略映射：按 (TaskStatus, IdentityType) 集中推导 AppResult.allowedActions。
 */
@Component
public class AllowedActionResolver {

    public List<String> resolve(TaskStatus status, IdentityType identity) {
        if (status == null || identity == null) {
            return List.of();
        }
        return switch (status) {
            case DRAFT -> switch (identity) {
                case TASK_MANAGER -> List.of("发布任务", "修改任务", "撤回任务");
                default -> List.of();
            };
            case PUBLISHED -> switch (identity) {
                case TASK_MANAGER -> List.of("修改任务", "查看进度", "撤回任务");
                case ANNOTATOR -> List.of("阅读指南", "保存草稿", "提交切分");
                case ARBITER -> List.of("查看进度");
                default -> List.of();
            };
            case CUTTING_PENDING -> switch (identity) {
                case TASK_MANAGER -> List.of("查看进度", "重开本轮");
                case ANNOTATOR -> List.of("查看已提交切分");
                case ARBITER -> List.of("比对标注", "裁定冲突", "保存统一切分");
                default -> List.of();
            };
            case CUTTING_CONFIRMED -> switch (identity) {
                case TASK_MANAGER -> List.of("查看进度", "重开本轮");
                case ANNOTATOR -> List.of("保存草稿", "提交结构化标注");
                case ARBITER -> List.of("查看统一切分");
                default -> List.of();
            };
            case STRUCTURED_PENDING -> switch (identity) {
                case TASK_MANAGER -> List.of("查看进度", "重开本轮");
                case ANNOTATOR -> List.of("查看已提交标注");
                case ARBITER -> List.of("比对标注", "裁定冲突", "图示审查", "确认最终版本");
                default -> List.of();
            };
            case REOPENED -> switch (identity) {
                case TASK_MANAGER -> List.of("查看进度");
                case ANNOTATOR -> List.of("继续修改并重新提交");
                case ARBITER -> List.of("查看历史裁定");
                default -> List.of();
            };
            case CONFIRMED -> switch (identity) {
                case TASK_MANAGER -> List.of("查看最终版本", "导出数据");
                case ANNOTATOR, ARBITER, ADMIN -> List.of("查看最终版本");
                default -> List.of();
            };
            case WITHDRAWN -> switch (identity) {
                case TASK_MANAGER -> List.of("查看撤回原因");
                default -> List.of();
            };
        };
    }
}
