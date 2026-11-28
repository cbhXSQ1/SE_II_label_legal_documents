package com.seii.legal.data.entity;

import java.time.LocalDateTime;

public record CaseInfo(
        String documentId,
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
        String judgeGist,
        boolean autoParsed,
        boolean manualEdited,
        String filledBy,
        LocalDateTime filledAt
) {
}
