package com.seii.legal.function.dto.adjudication;

import java.util.List;

public record DiagramReviewCommand(
        String taskId,
        String documentId,
        String diagramId,
        Boolean passed,
        List<DiagramError> errors
) {
}
