package com.seii.legal.function.dto;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.data.entity.AdjudicationRecord;
import com.seii.legal.data.entity.DiagramReview;
import com.seii.legal.data.entity.DiffItem;
import com.seii.legal.function.service.AnnotationPayloadAdapter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class AdjudicationDtos {

    private AdjudicationDtos() {
    }

    public record CompareRequest(
            @NotBlank(message = "任务标识不能为空") String taskId,
            @NotBlank(message = "文书标识不能为空") String documentId,
            @NotNull(message = "轮次不能为空") RoundType round,
            @NotBlank(message = "请选择标注员甲") String annotatorA,
            @NotBlank(message = "请选择标注员乙") String annotatorB
    ) {
    }

    public record CompareResponse(
            double agreement,
            double difference,
            String versionA,
            String versionB,
            List<DiffItem> items
    ) {
    }

    public record ConflictsResponse(
            List<AdjudicationRecord> conflicts,
            long decidedCount,
            long undecidedCount
    ) {
    }

    public record DecideRequest(
            @NotBlank(message = "任务标识不能为空") String taskId,
            @NotBlank(message = "文书标识不能为空") String documentId,
            @NotNull(message = "轮次不能为空") RoundType round,
            @NotBlank(message = "冲突项标识不能为空") String conflictId,
            String differenceType,
            String differenceObject,
            String valueA,
            String valueB,
            @NotBlank(message = "请选择裁定方式") String method,
            String result,
            String expertReplyId
    ) {
    }

    public record ExpertQuestionRequest(
            @NotBlank(message = "任务标识不能为空") String taskId,
            String documentId,
            @NotBlank(message = "冲突项标识不能为空") String conflictId,
            String guideClause,
            String conflictLabel,
            String annotatorResults,
            @NotBlank(message = "请选择领域专家") String expertId,
            String note
    ) {
    }

    public record ExpertQuestionResponse(String questionId) {
    }

    public record ExpertReplyRequest(
            @NotBlank(message = "问题标识不能为空") String questionId,
            String conclusion,
            String reason,
            String referenceBasis,
            Boolean needMore,
            Boolean unanswerable
    ) {
    }

    public record DiagramReviewRequest(
            @NotBlank(message = "任务标识不能为空") String taskId,
            @NotBlank(message = "文书标识不能为空") String documentId,
            @NotBlank(message = "图示标识不能为空") String diagramId,
            boolean passed,
            List<DiagramReview.DiagramError> errors
    ) {
    }

    public record FinalVersionResponse(
            AnnotationPayloadAdapter.AnnotationContent content,
            String guideVersion,
            String confirmedBy,
            String confirmedAt,
            String status
    ) {
    }

    public record FinalConfirmRequest(
            @NotBlank(message = "任务标识不能为空") String taskId,
            @NotBlank(message = "文书标识不能为空") String documentId
    ) {
    }
}
