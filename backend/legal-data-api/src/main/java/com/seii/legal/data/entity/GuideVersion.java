package com.seii.legal.data.entity;

import com.seii.legal.common.enums.RoundType;
import java.time.LocalDateTime;
import java.util.List;

public record GuideVersion(
        String version,
        String scope,
        LocalDateTime importedAt,
        String operatorId,
        String status,
        List<String> tags,
        List<String> relationTypes,
        String diagramRules,
        String fileId
) {
}
