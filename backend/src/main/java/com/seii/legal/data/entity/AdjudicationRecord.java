package com.seii.legal.data.entity;

import com.seii.legal.common.enums.RoundType;

import java.time.LocalDateTime;

public record AdjudicationRecord(
        String decisionId,
        String taskId,
        String documentId,
        RoundType round,
        String conflictId,
        String differenceType,
        String differenceObject,
        String valueA,
        String valueB,
        String method,
        String result,
        String arbiterId,
        LocalDateTime decidedAt,
        String expertReplyId
) {
}
