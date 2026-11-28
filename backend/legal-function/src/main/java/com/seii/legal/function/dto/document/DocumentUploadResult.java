package com.seii.legal.function.dto.document;

public record DocumentUploadResult(
        String documentId,
        String title,
        String caseNo,
        String reasonText,
        CaseInfoView caseInfo,
        boolean duplicated,
        String duplicateDocumentId
) {
}
