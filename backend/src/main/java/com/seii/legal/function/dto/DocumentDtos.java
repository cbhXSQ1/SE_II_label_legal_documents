package com.seii.legal.function.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class DocumentDtos {

    private DocumentDtos() {
    }

    public record UploadDocumentRequest(
            String fileId,
            @NotBlank(message = "请选择文书类型") String type,
            String title,
            String caseNo,
            String rawText
    ) {
    }

    public record CaseInfoPayload(
            String caseNo,
            String court,
            String level,
            String trial,
            String date,
            String nature,
            String cause,
            String resultType,
            String caseType,
            String caseName,
            String publishDate,
            String storageNo,
            String relatedLaws,
            String judgePoints,
            String judgeGist
    ) {
    }

    public record UploadDocumentResponse(
            String documentId,
            String title,
            String caseNo,
            String type,
            String reasonText,
            CaseInfoPayload caseInfo,
            boolean duplicated,
            String duplicateDocumentId
    ) {
    }

    public record DocumentDetailResponse(
            String id,
            String title,
            String caseNo,
            String type,
            String reasonText,
            CaseInfoPayload caseInfo,
            String filledBy,
            String filledAt
    ) {
    }

    public record SaveCaseInfoRequest(
            @NotNull(message = "案件信息不能为空") CaseInfoPayload caseInfo
    ) {
    }

    public record DocumentSummary(
            String id,
            String title,
            String caseNo,
            String type,
            String taskId
    ) {
    }
}
