package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record GuideVersion(
        String version,
        String scope,
        LocalDateTime importedAt,
        String operatorId,
        String status,
        List<String> nonPropositionTypes,
        List<String> propositionTypes,
        List<String> relationTypes,
        List<String> diagramRules,
        String sourceFileId
) {
}
