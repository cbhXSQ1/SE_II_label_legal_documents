package com.seii.legal.data.entity;

import com.seii.legal.common.enums.VersionType;

import java.time.LocalDateTime;

public record FinalVersion(
        String finalVersionId,
        String taskId,
        String documentId,
        VersionType versionType,
        String cutVersionId,
        String elementVersionId,
        String relationVersionId,
        String diagramId,
        String contentJson,
        String guideVersion,
        String confirmedBy,
        LocalDateTime confirmedAt,
        String status
) {
}
