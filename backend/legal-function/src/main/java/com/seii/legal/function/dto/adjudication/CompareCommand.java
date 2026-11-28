package com.seii.legal.function.dto.adjudication;

import com.seii.legal.common.enums.RoundType;

public record CompareCommand(
        String taskId,
        String documentId,
        RoundType round,
        String annotatorA,
        String annotatorB
) {
}
