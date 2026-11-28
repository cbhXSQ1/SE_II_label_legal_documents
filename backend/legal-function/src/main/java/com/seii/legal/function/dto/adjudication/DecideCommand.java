package com.seii.legal.function.dto.adjudication;

import com.seii.legal.common.enums.RoundType;

public record DecideCommand(
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
        String expertReplyId
) {
}
