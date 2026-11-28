package com.seii.legal.function.dto.document;

import java.time.LocalDateTime;

public record DocumentDetailView(
        String id,
        String title,
        String caseNo,
        String type,
        String reasonText,
        CaseInfoView caseInfo,
        String filledBy,
        LocalDateTime filledAt
) {
}
