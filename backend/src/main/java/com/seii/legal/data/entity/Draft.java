package com.seii.legal.data.entity;

import com.seii.legal.common.enums.RoundType;

import java.time.LocalDateTime;

public record Draft(
        String taskId,
        String documentId,
        String annotatorId,
        RoundType round,
        String cutJson,
        String elementsJson,
        String relationsJson,
        String diagramJson,
        String contentJson,
        LocalDateTime savedAt
) {
}
