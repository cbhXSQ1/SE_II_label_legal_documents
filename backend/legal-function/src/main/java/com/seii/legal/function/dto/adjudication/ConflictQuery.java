package com.seii.legal.function.dto.adjudication;

import com.seii.legal.common.enums.RoundType;

public record ConflictQuery(
        String taskId,
        String documentId,
        RoundType round
) {
}
