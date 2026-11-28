package com.seii.legal.function.dto;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.function.service.AnnotationPayloadAdapter;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public final class AnnotationDtos {

    private AnnotationDtos() {
    }

    public record TaskGuideResponse(
            String version,
            String scope,
            boolean confirmed
    ) {
    }

    public record ConfirmGuideRequest(
            @NotBlank(message = "任务标识不能为空") String taskId,
            boolean confirmed
    ) {
    }

    public record LoadAnnotationResponse(
            AnnotationPayloadAdapter.AnnotationContent content,
            boolean submitted,
            RoundType round
    ) {
    }

    /**
     * 同时兼容嵌套 content 与前端顶层平铺 4 字段（pieces/segments/relations/diagram）。
     */
    public record SaveDraftRequest(
            @NotBlank(message = "任务标识不能为空") String taskId,
            @NotBlank(message = "文书标识不能为空") String documentId,
            RoundType round,
            AnnotationPayloadAdapter.AnnotationContent content,
            List<Map<String, Object>> pieces,
            List<Map<String, Object>> segments,
            List<Map<String, Object>> relations,
            List<Map<String, Object>> diagram
    ) {
        public AnnotationPayloadAdapter.AnnotationContent flatContent() {
            if (pieces == null && segments == null && relations == null && diagram == null) {
                return null;
            }
            return new AnnotationPayloadAdapter.AnnotationContent(pieces, segments, relations, diagram);
        }
    }

    public record SaveDraftResponse(String savedAt) {
    }

    public record SubmitAnnotationRequest(
            @NotBlank(message = "任务标识不能为空") String taskId,
            @NotBlank(message = "文书标识不能为空") String documentId,
            RoundType round,
            AnnotationPayloadAdapter.AnnotationContent content,
            List<Map<String, Object>> pieces,
            List<Map<String, Object>> segments,
            List<Map<String, Object>> relations,
            List<Map<String, Object>> diagram,
            String requestId
    ) {
        public AnnotationPayloadAdapter.AnnotationContent flatContent() {
            if (pieces == null && segments == null && relations == null && diagram == null) {
                return null;
            }
            return new AnnotationPayloadAdapter.AnnotationContent(pieces, segments, relations, diagram);
        }
    }

    public record SubmitAnnotationResponse(
            String submissionId,
            String submittedAt
    ) {
    }
}
