package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record DiagramReview(
        String reviewId,
        String taskId,
        String documentId,
        String diagramId,
        String reviewerId,
        LocalDateTime reviewedAt,
        boolean passed,
        List<DiagramError> errors
) {
    public record DiagramError(String position, String type, String note) {
    }
}
