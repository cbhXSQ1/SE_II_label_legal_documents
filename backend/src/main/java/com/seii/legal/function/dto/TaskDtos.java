package com.seii.legal.function.dto;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class TaskDtos {

    private TaskDtos() {
    }

    public record CreateTaskRequest(
            @NotBlank(message = "请填写任务名称") String name,
            String description,
            String deadline,
            @NotBlank(message = "请选择指南版本") String guideVersion,
            @NotBlank(message = "请选择文书") String documentId,
            List<String> annotatorIds,
            String arbiterId,
            boolean draft
    ) {
    }

    public record CreateTaskResponse(String taskId) {
    }

    public record UpdateTaskRequest(
            String description,
            String deadline,
            List<String> annotatorIds,
            String arbiterId,
            String arbiterReason
    ) {
    }

    public record WithdrawTaskRequest(
            @NotBlank(message = "请填写撤回原因") String reason
    ) {
    }

    public record ReopenTaskRequest(
            @NotNull(message = "请指定重开轮次") RoundType round,
            @NotBlank(message = "请填写重开原因") String reason
    ) {
    }

    public record MyTaskItem(
            String id,
            String name,
            String guide,
            String due,
            TaskStatus status,
            boolean teaching,
            List<String> annotators,
            String arbiter,
            String documentId
    ) {
    }

    public record MyTasksResponse(List<MyTaskItem> tasks) {
    }

    public record TaskDetailResponse(
            String id,
            String name,
            String description,
            String deadline,
            String guideVersion,
            String documentId,
            List<String> annotators,
            String arbiter,
            TaskStatus status,
            String publisher,
            String publishedAt,
            String withdrawReason
    ) {
    }

    public record AnnotatorProgressItem(
            String id,
            String name,
            String submittedAt,
            String status
    ) {
    }

    public record TaskProgressResponse(
            int due,
            int submitted,
            int unsubmitted,
            int conflictCount,
            int confirmedCount,
            List<AnnotatorProgressItem> annotators
    ) {
    }
}
