package com.seii.legal.data.entity;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.VersionType;
import java.time.LocalDateTime;
import java.util.List;

public record FinalVersion(
        String finalVersionId,
        String taskId,
        String documentId,
        String roundId,
        VersionType versionType,
        String cutVersionId,
        String elementVersionId,
        String relationVersionId,
        String diagramId,
        String guideVersion,
        String confirmedBy,
        LocalDateTime confirmedAt,
        String status
) {
}
