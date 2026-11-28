package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record Draft(
        String taskId,
        String documentId,
        String annotatorId,
        String roundId,
        String cutJson,
        String elementsJson,
        String relationsJson,
        String diagramJson,
        LocalDateTime savedAt
) {
}
