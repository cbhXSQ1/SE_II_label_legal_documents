package com.seii.legal.data.entity;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.VersionType;
import java.time.LocalDateTime;
import java.util.List;

public record AdjudicationRecord(
        String decisionId,
        String taskId,
        String documentId,
        RoundType round,
        String roundId,
        String conflictId,
        String differenceType,
        String differenceObject,
        String valueA,
        String valueB,
        String method,
        String result,
        String arbiterId,
        LocalDateTime decidedAt,
        String elementVersionId,
        String relationVersionId,
        String diagramId,
        String expertReplyId
) {
}
